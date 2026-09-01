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

---

# Deploying the new credential to the FIX host on AWS

This section covers the actual cutover on the AWS-hosted FIX engine. It is written
without sight of that environment, so the discovery steps come first — run them and
the rest becomes specific.

## The three things that break this cutover

These are the failure modes worth knowing before touching anything. None of them
are about the certificate itself.

**1. Two sessions with the same CompID.** A FIX session is a singleton. If a
rolling deployment starts the new task before the old one stops, both present
`MAP_TRYG_PROD` at once. Bloomberg drops one or both, and sequence numbers can end
up inconsistent. On ECS this means forcing stop-then-start rather than the default
rolling update:

```
--deployment-configuration "minimumHealthyPercent=0,maximumPercent=100"
```

On EC2 with systemd, `systemctl restart` is already stop-then-start, so this is a
non-issue there. Accept the few seconds of downtime; it is the safe shape.

**2. A lost message store resets sequence numbers.** QuickFIX/J keeps sequence
state on disk (`FileStorePath`). If that path is inside an ephemeral container
filesystem, a redeploy starts from sequence 1, Bloomberg expects continuity, and
the session fails to establish until sequence numbers are reconciled. Confirm the
store is on persistent storage (EBS volume, or EFS mount for ECS) *before*
restarting. If it is not, arrange a sequence reset with Bloomberg as part of the
window rather than discovering it mid-cutover.

**3. The egress IP is registered with Bloomberg.** Bloomberg allowlists the source
IP. If the FIX host reaches them through a NAT gateway with an Elastic IP, that EIP
is what they know. Replacing an instance or task is safe as long as egress still
leaves via the same NAT/EIP; moving subnets or AZs may not be. Check before, not
after:

```
# from the FIX host itself
curl -s https://checkip.amazonaws.com
```

Compare that against the IP registered in the Bloomberg console. A certificate swap
does not change this, but a host rebuild during the same window would.

## Step 1 — find the host and the engine

```
# EC2 instances tagged for FIX (adjust the tag filter to local convention)
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=*fix*" \
  --query 'Reservations[].Instances[].{Id:InstanceId,Name:Tags[?Key==`Name`]|[0].Value,State:State.Name,PrivateIp:PrivateIpAddress}' \
  --output table

# ECS services, if it runs as a task instead
aws ecs list-clusters
aws ecs list-services --cluster <cluster>
```

Then connect — prefer SSM Session Manager over SSH, no inbound port needed:

```
aws ssm start-session --target <instance-id>
```

On the host, identify the engine and its config:

```
systemctl list-units --type=service | grep -i fix
ps -ef | grep -iE 'quickfix|fix|java' | grep -v grep
# the config path is usually a -D property or a CLI arg on the java process
```

## Step 2 — locate the current keystore and password

In a QuickFIX/J config (`.cfg`) the relevant keys are:

```
SocketUseSSL=Y
SocketKeyStore=/opt/fix/certs/cert.jks
SocketKeyStorePassword=...
SocketTrustStore=/opt/fix/certs/truststore.jks
SocketTrustStorePassword=...
```

The password may instead arrive from Secrets Manager or SSM Parameter Store at
startup. Check the unit file or task definition:

```
systemctl cat <fix-service> | grep -iE 'secret|ssm|environment|ExecStart'
aws ecs describe-task-definition --task-definition <family> \
  --query 'taskDefinition.containerDefinitions[].secrets'
```

Whichever it is, the new bundle has its **own** password — a different 22-character
string from the current one. Updating the keystore without updating the password is
the most likely way to fail this cutover.

## Step 3 — back up, then stage

```
sudo cp -a /opt/fix/certs /opt/fix/certs.bak-$(date +%Y%m%d)
```

Keep the old keystore in place under a distinct name. Do not overwrite it — it is
the rollback.

Copy the new bundle to the host (via S3 with a short-lived object, or SSM, not by
pasting a private key into a terminal that logs), then validate it in situ:

```
scripts/verify-bbg-cert-bundle.sh /path/to/extracted-bundle
scripts/mtls-smoke-test.sh        /path/to/extracted-bundle
```

Both must pass before the restart. They take seconds and catch a wrong or truncated
bundle while rollback is still trivial.

## Step 4 — update the secret, then the config

If the password lives in Secrets Manager:

```
aws secretsmanager put-secret-value \
  --secret-id <fix/keystore/password> \
  --secret-string file://password.txt
```

If in SSM Parameter Store:

```
aws ssm put-parameter --name /fix/keystore/password \
  --value "$(cat password.txt)" --type SecureString --overwrite
```

Then point the config at the new keystore. Keep the old value commented beside it so
the rollback is a one-line edit rather than a memory exercise.

Delete the local `password.txt` and the extracted bundle from the host afterwards.

## Step 5 — restart in the window

```
# EC2 / systemd
sudo systemctl restart <fix-service>
sudo journalctl -u <fix-service> -f

# ECS — register the new revision, then force stop-then-start
aws ecs update-service --cluster <cluster> --service <svc> \
  --task-definition <family>:<new-revision> \
  --deployment-configuration "minimumHealthyPercent=0,maximumPercent=100" \
  --force-new-deployment
```

## Step 6 — verify

Three independent confirmations, in order:

1. **TLS handshake succeeded** — the log shows no `SSLHandshakeException`, no
   `bad_certificate` alert.
2. **Logon accepted** — a FIX `Logon (35=A)` sent and a `Logon` received back, then
   `Heartbeat (35=0)` exchange settling into rhythm. A handshake without a logon
   means Bloomberg accepted the TLS but rejected the session.
3. **Bloomberg console agrees** — `610146:5` **Last Usage Time** starts advancing
   and `610146:4` stops. This is the authoritative confirmation; the logs only show
   our side of it.

Watch through at least two heartbeat intervals before calling it done. An immediate
logon followed by a disconnect thirty seconds later is a different failure than a
clean logon.

## Rollback

Point the config back at the backed-up keystore, restore the old password in the
secret store, restart. Valid until 2026-09-04, and only until then — which is the
reason for going before the deadline rather than after it.

## After it is stable

Ask Bloomberg to deactivate `610146:4`. The daily Critical alert stops when the
SHA-1 credential is gone, not when the new one starts being used.
