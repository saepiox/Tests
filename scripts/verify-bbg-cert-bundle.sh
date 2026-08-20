#!/usr/bin/env bash
# Offline validation of a Bloomberg FIX credential bundle (the ZIP downloaded
# from https://ec.bloomberg.com/fix/manage/<FIXID>/credentials).
#
# Checks everything that can be checked WITHOUT touching Bloomberg:
#   - signature algorithm is SHA-256 (not the deprecated SHA-1)
#   - key size, validity window, days remaining
#   - private key actually matches the certificate
#   - the cert chains to the bundled CA set
#   - the JKS / PKCS#12 stores open with the bundled password
#
# Usage: scripts/verify-bbg-cert-bundle.sh <extracted-bundle-dir>
#        (the directory containing pem/, jks/ and pkcs12/)

set -uo pipefail

BUNDLE="${1:-}"
if [[ -z "$BUNDLE" || ! -d "$BUNDLE" ]]; then
    echo "usage: $0 <extracted-bundle-dir>" >&2
    exit 2
fi

CERT="$BUNDLE/pem/cert.pem"
KEY="$BUNDLE/pem/key.pem"
CAS="$BUNDLE/pem/CACerts.pem"

fail=0
ok()   { printf '  \033[32mPASS\033[0m  %s\n' "$1"; }
bad()  { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; fail=1; }
warn() { printf '  \033[33mWARN\033[0m  %s\n' "$1"; }

for f in "$CERT" "$KEY" "$CAS"; do
    [[ -f "$f" ]] || { echo "missing expected file: $f" >&2; exit 2; }
done

echo "== Certificate =="
subject=$(openssl x509 -in "$CERT" -noout -subject | sed 's/^subject=//')
serial=$(openssl x509 -in "$CERT" -noout -serial | sed 's/^serial=//')
notafter=$(openssl x509 -in "$CERT" -noout -enddate | sed 's/^notAfter=//')
echo "  subject : $subject"
echo "  serial  : $serial"
echo "  expires : $notafter"

# --- signature algorithm: the whole point of this migration ---------------
sigalg=$(openssl x509 -in "$CERT" -noout -text | awk '/Signature Algorithm/{print $3; exit}')
if [[ "$sigalg" == sha1* ]]; then
    bad "signature algorithm is $sigalg - Bloomberg is deprecating SHA-1"
elif [[ "$sigalg" == sha256* || "$sigalg" == sha384* || "$sigalg" == sha512* ]]; then
    ok "signature algorithm is $sigalg"
else
    warn "unrecognised signature algorithm: $sigalg"
fi

# --- key size --------------------------------------------------------------
bits=$(openssl x509 -in "$CERT" -noout -text | awk -F'[()]' '/Public-Key:/{print $2}' | awk '{print $1}')
if [[ -n "$bits" && "$bits" -ge 2048 ]]; then
    ok "public key is ${bits}-bit"
else
    bad "public key is ${bits:-unknown}-bit (expected >= 2048)"
fi

# --- validity window -------------------------------------------------------
if openssl x509 -in "$CERT" -noout -checkend 0 >/dev/null 2>&1; then
    # -checkend takes seconds; 30 days = 2592000
    if openssl x509 -in "$CERT" -noout -checkend 2592000 >/dev/null 2>&1; then
        ok "certificate is valid for more than 30 days"
    else
        warn "certificate expires within 30 days ($notafter)"
    fi
else
    bad "certificate has already expired ($notafter)"
fi

# --- key/cert pairing ------------------------------------------------------
cert_mod=$(openssl x509 -in "$CERT" -noout -modulus 2>/dev/null | openssl md5)
key_mod=$(openssl rsa  -in "$KEY"  -noout -modulus 2>/dev/null | openssl md5)
if [[ -n "$cert_mod" && "$cert_mod" == "$key_mod" ]]; then
    ok "private key matches the certificate"
else
    bad "private key does NOT match the certificate"
fi

# --- chain -----------------------------------------------------------------
echo
echo "== Chain =="
if openssl verify -CAfile "$CAS" "$CERT" >/dev/null 2>&1; then
    ok "certificate chains to the bundled CA set"
else
    bad "certificate does not verify against $CAS"
    openssl verify -CAfile "$CAS" "$CERT" 2>&1 | sed 's/^/        /'
fi

# Flag CA entries that expire soon - a trust anchor ageing out breaks the
# handshake just as surely as an expired leaf.
now=$(date -u +%s)
csplit -z -s -f "${TMPDIR:-/tmp}/_bbgca_" -b '%02d.pem' "$CAS" '/BEGIN CERTIFICATE/' '{*}' 2>/dev/null
for ca in "${TMPDIR:-/tmp}"/_bbgca_*.pem; do
    [[ -f "$ca" ]] || continue
    ca_sub=$(openssl x509 -in "$ca" -noout -subject 2>/dev/null | sed 's/.*CN *= *//; s/,.*//')
    ca_end=$(openssl x509 -in "$ca" -noout -enddate 2>/dev/null | sed 's/^notAfter=//')
    ca_end_s=$(date -u -d "$ca_end" +%s 2>/dev/null) || { rm -f "$ca"; continue; }
    days=$(( (ca_end_s - now) / 86400 ))
    if   (( days < 0 ));   then bad  "CA '$ca_sub' EXPIRED ($ca_end)"
    elif (( days < 180 )); then warn "CA '$ca_sub' expires in $days days ($ca_end)"
    else                        ok   "CA '$ca_sub' valid for $days more days"
    fi
    rm -f "$ca"
done

# --- keystores -------------------------------------------------------------
echo
echo "== Keystores =="
pfx="$BUNDLE/pkcs12/cert.pfx"; pfx_pw="$BUNDLE/pkcs12/password.txt"
if [[ -f "$pfx" && -f "$pfx_pw" ]]; then
    if openssl pkcs12 -in "$pfx" -nokeys -noout -passin "file:$pfx_pw" >/dev/null 2>&1; then
        ok "PKCS#12 store opens with the bundled password"
    else
        bad "PKCS#12 store does not open with the bundled password"
    fi
else
    warn "no pkcs12/cert.pfx in bundle - skipped"
fi

jks="$BUNDLE/jks/cert.jks"; jks_pw="$BUNDLE/jks/password.txt"
if [[ -f "$jks" && -f "$jks_pw" ]]; then
    if command -v keytool >/dev/null 2>&1; then
        if keytool -list -keystore "$jks" -storepass "$(cat "$jks_pw")" >/dev/null 2>&1; then
            ok "JKS store opens with the bundled password"
        else
            bad "JKS store does not open with the bundled password"
        fi
    else
        warn "keytool not on PATH - JKS not checked"
    fi
else
    warn "no jks/cert.jks in bundle - skipped"
fi

# --- known gotcha ----------------------------------------------------------
if [[ -f "$BUNDLE/pem/cert_all.pem" ]] && grep -q 'BEGIN CA CERTIFICATES' "$BUNDLE/pem/cert_all.pem"; then
    echo
    warn "cert_all.pem uses non-standard '-----BEGIN CA CERTIFICATES-----' markers;"
    warn "OpenSSL and several FIX engines cannot parse it. Use cert.pem + key.pem"
    warn "+ CACerts.pem separately, or the PKCS#12 / JKS store instead."
fi

echo
if (( fail )); then
    echo "RESULT: bundle FAILED validation"
    exit 1
fi
echo "RESULT: bundle passed validation"
