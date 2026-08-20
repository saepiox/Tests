#!/usr/bin/env bash
# Local mutual-TLS smoke test for a Bloomberg FIX credential bundle.
#
# Stands up a throwaway TLS server on localhost that demands a client
# certificate signed by Bloomberg's CA, then connects to it using the bundle's
# certificate and key. A successful handshake proves the cert, the key, the
# chain and the local TLS stack all work together.
#
# This does NOT prove Bloomberg will accept the credential - only Bloomberg's
# own UAT/PROD endpoint can prove that. What it does do is catch every failure
# that is on our side of the wire (wrong key, truncated chain, unreadable
# keystore, TLS version mismatch) without needing any Bloomberg connectivity.
#
# Usage: scripts/mtls-smoke-test.sh <extracted-bundle-dir> [port]

set -uo pipefail

BUNDLE="${1:-}"
PORT="${2:-14443}"

if [[ -z "$BUNDLE" || ! -d "$BUNDLE" ]]; then
    echo "usage: $0 <extracted-bundle-dir> [port]" >&2
    exit 2
fi

CERT="$BUNDLE/pem/cert.pem"
KEY="$BUNDLE/pem/key.pem"
CAS="$BUNDLE/pem/CACerts.pem"

for f in "$CERT" "$KEY" "$CAS"; do
    [[ -f "$f" ]] || { echo "missing expected file: $f" >&2; exit 2; }
done

WORK=$(mktemp -d)
cleanup() {
    [[ -n "${SRV_PID:-}" ]] && kill "$SRV_PID" 2>/dev/null
    wait "${SRV_PID:-}" 2>/dev/null
    rm -rf "$WORK"
}
trap cleanup EXIT

# A self-signed server identity, standing in for Bloomberg's endpoint. The
# client does not verify it; only the client-auth direction is under test.
openssl req -x509 -newkey rsa:2048 -nodes -days 1 \
    -subj "/CN=bbg-fix-stub.localhost" \
    -keyout "$WORK/server.key" -out "$WORK/server.crt" >/dev/null 2>&1 \
    || { echo "could not generate stub server identity" >&2; exit 1; }

# -Verify (capital V) makes client auth mandatory. -verify_return_error is what
# makes a verification failure actually fatal: without it s_server logs
# "verify error" and completes the handshake anyway, which would let an
# untrusted certificate pass this test.
openssl s_server \
    -accept "$PORT" -naccept 1 \
    -cert "$WORK/server.crt" -key "$WORK/server.key" \
    -CAfile "$CAS" -Verify 3 -verify_return_error \
    -tls1_2 -quiet > "$WORK/server.log" 2>&1 &
SRV_PID=$!

# Wait for the listener rather than sleeping blindly.
for _ in $(seq 1 50); do
    if grep -q . "$WORK/server.log" 2>/dev/null; then break; fi
    if command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | grep -q ":$PORT "; then break; fi
    kill -0 "$SRV_PID" 2>/dev/null || break
    read -r -t 0.1 < /dev/zero 2>/dev/null || true
done

if ! kill -0 "$SRV_PID" 2>/dev/null; then
    echo "stub server failed to start (is port $PORT in use?)" >&2
    sed 's/^/    /' "$WORK/server.log" >&2
    exit 1
fi

echo "== Mutual TLS handshake against local stub (port $PORT) =="
# The client trusts the stub server's own cert: the server identity is not what
# is under test here, our client credential is. Pointing -CAfile at the stub
# cert keeps the server side of the verification out of the verdict.
client_out=$(echo "Q" | openssl s_client \
    -connect "localhost:$PORT" \
    -cert "$CERT" -key "$KEY" \
    -CAfile "$WORK/server.crt" \
    -tls1_2 2>&1)

# Give the server a moment to flush its verification trace before reading it.
wait "$SRV_PID" 2>/dev/null
server_out=$(cat "$WORK/server.log" 2>/dev/null)

# The credential passes only if BOTH sides agree: the client completed a
# handshake, and the server verified our leaf certificate at depth 0.
client_ok=0
grep -q "Verify return code: 0 (ok)" <<<"$client_out" \
    && ! grep -qi "handshake failure\|no peer certificate\|alert " <<<"$client_out" \
    && client_ok=1

leaf_cn=$(openssl x509 -in "$CERT" -noout -subject | sed 's/.*CN *= *//; s/,.*//')
server_ok=0
grep -q "depth=0.*CN *= *$leaf_cn" <<<"$server_out" \
    && ! grep -q "verify error" <<<"$server_out" \
    && server_ok=1

if (( client_ok && server_ok )); then
    cipher=$(grep -m1 "Cipher    :" <<<"$client_out" | sed 's/.*: *//')
    proto=$(grep -m1 "Protocol  :" <<<"$client_out" | sed 's/.*: *//')
    printf '  \033[32mPASS\033[0m  handshake succeeded (%s, %s)\n' "${proto:-?}" "${cipher:-?}"
    printf '  \033[32mPASS\033[0m  server verified our client certificate (CN=%s)\n' "$leaf_cn"
    echo
    echo "The credential is loadable and usable for mutual TLS."
    echo "Bloomberg acceptance still has to be confirmed against their UAT endpoint."
    exit 0
fi

printf '  \033[31mFAIL\033[0m  handshake did not complete\n'
echo "--- client ---"; sed 's/^/    /' <<<"$client_out" | head -40
echo "--- server ---"; sed 's/^/    /' <<<"$server_out" | head -20
exit 1
