# Bloomberg FIX certificate upgrade — FIXID 610146 (MAP_TRYG_PROD)

Status: **replacement certificate already exists and is valid. It has not been
deployed.** No new certificate needs to be requested.

## The alert

Bloomberg's daily alert (Enterprise ID 40429, severity Critical) concerns the
credential currently in service:

| | |
|---|---|
| Common Name | `610146:4` |
| Serial | `efa01428540d89fc894f70aa80e94ea4` |
| Signature | SHA-1 (being deprecated) |
| Expires | **2026-09-04 23:59 UTC** |
| Last used | 2026-08-17 |
| CompIDs | `MAP_BBG_PROD` / `MAP_TRYG_PROD` |
| Environment | PROD |

## What the attached bundle actually contains

The ZIP attached to the mail (`MAP_TRYG_PROD-2026-02-27 (1).zip`) is **not** the
expiring credential. It is the replacement, issued on 2026-02-27:

| | |
|---|---|
| Common Name | `610146:5` |
| Serial | `01B9ECF87D537FD795BBC0DBADD1C073` |
| Signature | **sha256WithRSAEncryption** |
| Key | RSA 2048-bit |
| Valid | 2026-02-27 → **2028-02-27** |

Verified offline with `scripts/verify-bbg-cert-bundle.sh`:

- signature algorithm is SHA-256 — this is the "new specification" the alert asks for
- private key matches the certificate
- chain verifies cleanly against the bundled `CACerts.pem`
- both the JKS and the PKCS#12 store open with the bundled password

So the migration work was done in February and the new credential was simply
never put into service. The `:4` certificate showing a last-usage date of
2026-08-17 confirms the SHA-1 credential is still the one the FIX session is
presenting. That is why the alert fires every day.

## Do we need a clone of the Bloomberg FIX server on AWS?

No — and it would not tell us what we need to know.

The certificate is a *client* credential issued by Bloomberg's CA. What is being
tested is whether **Bloomberg** accepts it. A stub server we build on AWS would
accept whatever we configure it to accept, so a green result there proves
nothing about Bloomberg's side. The cost of standing it up buys no signal.

The two things worth testing are both cheaper:

1. **Our side of the wire** — that the credential loads, the key matches, the
   chain is complete and the TLS stack negotiates. `scripts/mtls-smoke-test.sh`
   does this in a few seconds against a throwaway local listener that demands a
   client certificate signed by Bloomberg's CA. No AWS, no Bloomberg
   connectivity, no cost. (It is a real test, not a formality — it correctly
   rejects a certificate Bloomberg did not sign.)

2. **Bloomberg's side** — only Bloomberg can answer this, and they already
   provide the environment for it. Use the Bloomberg **UAT/CERT** FIX endpoint
   rather than building our own.

There is also a safety net that removes most of the need for a rehearsal:
**Bloomberg allows multiple active credentials per FIXID.** `610146:4` and
`610146:5` can both be enabled at once. That makes the cutover reversible — if
`:5` fails, restart on `:4`, which stays valid until 2026-09-04.

## Recommended path

Deploy directly to PROD during a maintenance window, with `:4` kept enabled as
the rollback. Confirm in the Bloomberg console that both credentials are active
*before* changing anything on our side.

### Before the window

1. In the [Bloomberg console](https://ec.bloomberg.com/fix/manage/610146/credentials),
   confirm `610146:5` is **Active**, and that `610146:4` is still active as fallback.
2. Consider **re-downloading the bundle from the console** rather than using the
   one from the mail thread. The private key in the attachment has travelled by
   email through several mailboxes; its confidentiality can no longer be assumed.
   The `(1)` in the filename also suggests it is a re-download, so it may not be
   the newest issuance. If a fresh bundle is generated, re-run the verify script
   against it.
3. Run the offline checks:
   ```
   scripts/verify-bbg-cert-bundle.sh <extracted-bundle-dir>
   scripts/mtls-smoke-test.sh        <extracted-bundle-dir>
   ```
4. Back up the current keystore and FIX engine config.

### Cutover

5. Install the new credential on the FIX host. Keep the old keystore in place
   under a different filename — do not overwrite it.
6. Update the FIX engine config to point at the new keystore, and update the
   keystore password (the new bundle has its own 22-character password; it is
   **not** the same as the previous one).
7. Restart the FIX session and confirm logon.

### Verify

8. Confirm the session establishes and a heartbeat exchange completes.
9. In the console, check that **Last Usage Time** on `610146:5` updates and that
   `610146:4` stops advancing.
10. Once stable, ask Bloomberg to deactivate `610146:4`. The daily alert stops
    when the SHA-1 credential is gone.

### Rollback

Restore the previous keystore path and password, restart the session. Valid
until 2026-09-04.

## Two things that will bite during deployment

**`cert_all.pem` does not parse.** Bloomberg wraps the CA section in
non-standard `-----BEGIN CA CERTIFICATES-----` markers, which OpenSSL and
several FIX engines reject. Use `cert.pem` + `key.pem` + `CACerts.pem`
separately, or the PKCS#12 / JKS store. The verify script warns about this.

**A trust anchor expires the day after the leaf.** `CACerts.pem` still carries
the legacy `FIX Connectivity` CA, which expires **2026-09-05**, and the SHA-1
`System Security Root CA` behind it. Our new certificate chains through the
modern `Bloomberg Connectivity and Integration` path and does not depend on
these, but if the truststore anywhere in the estate still pins the legacy chain,
that will break in mid-September independently of this change. Worth checking
while the FIX config is already open.

## Timeline

The SHA-1 credential expires **2026-09-04** — about two weeks out. The
replacement is ready today, so this is a scheduling question, not an
engineering one. Deploying well before the deadline keeps `:4` available as a
working rollback; deploying after it removes that option entirely.

## Handling the credential material

The bundle contains a live PROD private key and keystore password. It must not
be committed to this repository or any other. `.gitignore` blocks the usual
filenames, but that is a backstop, not a control — move the bundle through the
normal secrets channel and delete local copies afterwards.
