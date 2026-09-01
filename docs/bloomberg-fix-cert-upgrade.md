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
| Signature | sha256WithRSAEncryption (see note below) |
| Expires | **2026-09-04 23:59 UTC** |
| Last used | 2026-08-17 |
| CompIDs | `MAP_BBG_PROD` / `MAP_TRYG_PROD` |
| Environment | PROD |

### Correction: the leaf certificate is not SHA-1

The alert's Detail text reads "SHA-1 Cryptographic Certificates are being
deprecated", but the deployed `610146:4` leaf is **sha256WithRSAEncryption**, and
its own X.509 `notAfter` is **2027-03-03**. Reading the certificate alone would
suggest there is another eighteen months of runway. There is not.

What is actually being retired is the **chain**:

| | Issuer chain | Root signature |
|---|---|---|
| `610146:4` (deployed) | `FIX Connectivity` → `System Security Root CA` | sha1WithRSAEncryption |
| `610146:5` (replacement) | `Bloomberg Connectivity and Integration FIX CA` → `... Root CA` | sha256WithRSAEncryption |

The legacy `FIX Connectivity` CA expires **2026-09-05**, and Bloomberg cuts the
credentials issued under it on **2026-09-04**. So the 09-04 date is an
administrative cutoff tied to the CA, not to the leaf's own validity window. Do
not let the leaf's 2027 expiry date reassure anyone.

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

# Deploying the new credential — the actual AWS setup

Discovered from the live environment, not assumed. This supersedes an earlier draft
of this document that guessed at a keystore on disk managed by systemd. That is not
how this runs.

## What is actually there

| | |
|---|---|
| Account / region | `513132248511` / `eu-central-1` |
| Instance | `i-0a3ccb5eb8eec0d03` (`t2.micro`, running) |
| Managed by | **Elastic Beanstalk** — app `BBGFix`, env `Bbgfix-prod` (`e-t4vvmazk3h`) |
| Platform | 64bit Amazon Linux 2018.03 v3.4.4, Tomcat 8.5, Java 8 |
| Live version | `bbgfix-source-11`, deployed 2025-04-04 |
| Artifact | `s3://elasticbeanstalk-eu-central-1-513132248511/1743756437899-multisession.war` |

There is no `BBGFIX` S3 bucket and there never was one. Elastic Beanstalk keeps
application bundles in its own `elasticbeanstalk-<region>-<account>` bucket, which
is where the deployable artifact lives.

## The thing that changes the whole procedure

**The certificates are inside the WAR**, not on the instance:

```
WEB-INF/classes/cert/trygprod/{pem,jks,pkcs12}/...
WEB-INF/classes/tryg_prod.cfg
```

So this is **not** a copy-a-file-and-restart job. Replacing the credential means
producing a new WAR and deploying it as a new Elastic Beanstalk application
version. Anything written onto the instance filesystem is discarded on the next
deploy, so it is not a fix.

The session config (`WEB-INF/classes/tryg_prod.cfg`) is a QuickFIX/J initiator:

```
SenderCompID=MAP_TRYG_PROD      TargetCompID=MAP_BBG_PROD
BeginString=FIX.4.4             SocketUseSSL=Y   EnabledProtocols=TLSv1.2
SocketConnectHost=69.191.198.2  SocketConnectPort=8228
SocketConnectHost1=69.191.230.2 SocketConnectPort1=8228   # failover
SocketKeyStore=cert/trygprod/jks/cert.jks
SocketKeyStorePassword=<cleartext in the file>
ResetOnLogon=Y                  HeartBtInt=60
StartTime=EndTime=17:00:00 America/New_York
```

Two consequences worth noting:

- `ResetOnLogon=Y` means sequence numbers reset on every logon. The usual concern
  about losing a message store on an ephemeral filesystem does **not** apply here.
- The keystore password is stored in cleartext in the config file, inside the WAR,
  in S3. See "Security observations" below.

## Swapping the leaf certificate alone will not work

The deployed `CACerts.pem` contains only the legacy chain (`FIX Connectivity` →
`System Security Root CA`). The replacement `610146:5` is issued under a different
chain, and the deployed truststore cannot validate it:

```
$ openssl verify -CAfile <deployed>/CACerts.pem <new>/cert.pem
error 20 at 0 depth lookup: unable to get local issuer certificate
```

Replace the **entire** `cert/trygprod/` directory from the new bundle — `cert.pem`,
`key.pem`, `CACerts.pem`, `cert.jks`, `cert.pfx` — not just the leaf. The new
bundle's `CACerts.pem` carries both the new and the legacy chains, so it stays
valid for the old credential during the rollback window.

And update `SocketKeyStorePassword` in `tryg_prod.cfg`: the new bundle ships its
own keystore password, which is **not** the current one.

## Two routes to a new WAR

**Route A — rebuild from source (correct).** The artifact is Maven
`com.saepiox:bbgfix-server:0.0.1-SNAPSHOT`. The WAR ships compiled classes only
(81 `.class` files, zero `.java`), so the source lives in another repository.
Replace the cert resources and the config there, build, deploy. This keeps the
repository as the source of truth.

**Route B — repackage the existing WAR (fast).** A WAR is a zip, and everything
that must change is a resource, not code. Swap `cert/trygprod/*` and
`tryg_prod.cfg` inside the existing artifact, then upload the result as a new
application version. No compiler, no source access.

Route B carries a real cost: the source repository still holds the old
certificate, so the next genuine build from source silently reverts the credential.
If Route B is used because of the deadline, raise a follow-up to land the same
change in source immediately afterwards.

## Deploying

Elastic Beanstalk deploys are the supported path. Do not edit files on the
instance.

```
# upload the new artifact
aws s3 cp multisession.war \
  s3://elasticbeanstalk-eu-central-1-513132248511/<new-key>.war

# register it as an application version
aws elasticbeanstalk create-application-version \
  --application-name BBGFix \
  --version-label bbgfix-source-12 \
  --source-bundle S3Bucket=elasticbeanstalk-eu-central-1-513132248511,S3Key=<new-key>.war

# deploy it
aws elasticbeanstalk update-environment \
  --environment-name Bbgfix-prod \
  --version-label bbgfix-source-12
```

Rollback is the same call with `--version-label bbgfix-source-11`, which is why
that version must not be deleted. This is a far cleaner rollback than restoring
files by hand, and it stays available until Bloomberg deactivates `610146:4`.

The environment is a single-instance autoscaling group, so a deploy stops and
starts the one instance. There is no window in which two logons share
`MAP_TRYG_PROD`. Expect a short disconnect and an automatic reconnect
(`ReconnectInterval=60`).

## Verifying

`FileLogPath=/var/log`, and the `.ebextensions` files pull the FIX event and
message logs into EB log bundles:

```
aws elasticbeanstalk request-environment-info  --environment-name Bbgfix-prod --info-type tail
aws elasticbeanstalk retrieve-environment-info --environment-name Bbgfix-prod --info-type tail
```

Look for a `Logon (35=A)` sent **and received back**, then heartbeats settling.
Then confirm in the Bloomberg console that `610146:5` Last Usage advances and
`610146:4` stops.

## Rehearsing: there is no sandbox

An earlier revision of this document suggested rehearsing on the existing beta
session. That was wrong, and the correction matters:

- **There is no BBGFix sandbox environment.** `Bbgfix-prod` is the only Elastic
  Beanstalk environment for this application. The `Saepiox-sandbox`,
  `Saepiox-env` and `Saepiox-Nov23` environments belong to a different
  application (`saepiox`) and have nothing to do with the FIX server.
- **The beta credential is long dead.** `cert/trygbeta/` holds `607887:1`
  (FIXID 607887, a different FIXID from production's 610146), which **expired
  2023-03-04** and was issued under the same legacy `FIX Connectivity` chain
  being retired. It cannot rehearse anything as it stands.

Making the beta path usable again means asking Bloomberg to issue a fresh
SHA-256 credential for FIXID 607887 and deploying a beta build to test with. That
is the right long-term answer, but it depends on Bloomberg's turnaround and is
unlikely to complete before 2026-09-04.

### The pre-flight test that is available now

Bloomberg's production endpoint will tell you whether it accepts the new
certificate chain, without starting a FIX session. A TLS handshake alone proves
client-certificate acceptance; no `Logon (35=A)` is sent, so no second session
ever claims `MAP_TRYG_PROD` and the live session is untouched.

It must run **from the instance**, because Bloomberg allowlists the source IP:

```
aws ssm start-session --target i-0a3ccb5eb8eec0d03      # SSM agent is Online
```

Then, with the new bundle staged in a temporary directory on the box:

```
openssl s_client -connect 69.191.198.2:8228 \
  -cert cert.pem -key key.pem -CAfile CACerts.pem -tls1_2 </dev/null
```

What the result means:

- Handshake completes, `Verify return code: 0 (ok)` — Bloomberg accepts the new
  credential. This is the assurance the cutover needs.
- `alert handshake failure` / `bad certificate` — Bloomberg rejects it. Stop, and
  take it to the helpdesk before deploying anything.

Delete the staged bundle from the instance afterwards; it contains the private
key. Note also that anything written to the instance filesystem is discarded on
the next deploy, so this staging is genuinely temporary.

Tell the Bloomberg helpdesk you intend to do this. It is an unsolicited connection
to a production venue, and it is better announced than explained afterwards.

### Realistic order of assurance

1. `scripts/verify-bbg-cert-bundle.sh` and `scripts/mtls-smoke-test.sh` — offline,
   seconds, catches a wrong or corrupt bundle.
2. The `openssl s_client` handshake above, from the instance — proves Bloomberg
   accepts the new chain.
3. Deploy to `Bbgfix-prod` in a window, with `bbgfix-source-11` ready to redeploy.

Steps 1 and 2 remove nearly all the risk that a rehearsal environment would have
removed. Step 3 is where the credential actually changes, and the rollback is what
makes it safe rather than a rehearsal would have.

## Security observations

Not blocking the cert swap, but worth scheduling afterwards:

- The production keystore password sits in cleartext in `tryg_prod.cfg` inside the
  WAR. Every copy of the artifact in S3 carries it, alongside the private key. The
  bucket is not public and is encrypted at rest (AES256), but it has no Block
  Public Access configuration.
- The platform — Amazon Linux 2018.03, Tomcat 8.5, Java 8 — is long out of
  support. A managed-platform update is separate work and should not be bundled
  into this cutover.
- A `t2.micro` is carrying a production trading connection.
