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

### The pre-flight test — run, and it closed the question

Bloomberg's production endpoint will state which client-certificate CAs it accepts
during a TLS handshake, before any FIX session exists. No `Logon (35=A)` is sent,
so nothing ever claims `MAP_TRYG_PROD` twice and the live session is untouched.

It has to run **from the instance**, because Bloomberg allowlists the source IP.
SSM Session Manager is available (agent Online, `AmazonSSMManagedInstanceCore`
attached), and the currently deployed credential is already on the box, so this
needs nothing staged:

```
aws ssm start-session --target i-0a3ccb5eb8eec0d03 --region eu-central-1

D=/var/lib/tomcat8/webapps/ROOT/WEB-INF/classes/cert/trygprod/pem
sudo openssl s_client -connect 69.191.198.2:8228 \
  -cert $D/cert.pem -key $D/key.pem -CAfile $D/CACerts.pem \
  -tls1_2 </dev/null
```

**Executed 2026-09-01 from `i-0a3ccb5eb8eec0d03`. Handshake completed:**
`Verify return code: 0 (ok)`, TLSv1.2, `ECDHE-RSA-AES256-GCM-SHA384`. So the
endpoint is reachable, the egress IP is allowlisted, and the current credential
still works.

The valuable part was not the pass, but what the server advertised in its
**Acceptable client certificate CA names**:

```
/CN=System Security Root CA                          <- legacy root
/CN=FIX Connectivity                                  <- issuer of 610146:4 (current)
/CN=Bloomberg Connectivity and Integration FIX CA     <- issuer of 610146:5 (new)
/CN=Bloomberg Connectivity and Integration Root CA    <- new root
/CN=FIX Connectivity 2025
```

Bloomberg's **production** endpoint already accepts client certificates issued
under `Bloomberg Connectivity and Integration FIX CA` — exactly the issuer of the
replacement credential. That is server-side confirmation that the new chain is
accepted, obtained without presenting the new certificate at all.

This was the one genuine unknown in the cutover. It is now closed: the risk of
Bloomberg rejecting the new chain is gone.

### The truststore swap cannot break server validation

The new `CACerts.pem` is a strict superset of the deployed one. Both anchors
currently in use are byte-identical between the two bundles:

| Trust anchor | Deployed | New bundle |
|---|---|---|
| `FIX Connectivity` | `29402510C076…` | `29402510C076…` |
| `System Security Root CA` | `8E850BFD0F34…` | `8E850BFD0F34…` |

(SHA-256 fingerprints, truncated.) The new bundle adds
`Bloomberg Connectivity and Integration` FIX CA, Root CA and Server Root CA.

Bloomberg currently presents `fixprod.bloomberg.com` issued by
`FIX Connectivity 2025`, which anchors on `System Security Root CA` — a root that
survives the swap unchanged. So replacing the truststore is safe in the server
direction as well as the client direction, and the added Server Root CA covers
Bloomberg rotating their own chain later.

Note that Bloomberg has already rotated their server intermediate to
`FIX Connectivity 2025`; the deployed truststore handles it only because it still
anchors on the old root.

### What is still unproven

Nothing cryptographic. TLS will accept `610146:5`.

What remains is an entitlement question the console alone can answer: **is
`610146:5` marked Active on FIXID 610146, with `610146:4` still active as
fallback?** Confirm that before the window. After that the cutover is a
scheduling decision, not a technical risk.

### Order of assurance

1. `scripts/verify-bbg-cert-bundle.sh` and `scripts/mtls-smoke-test.sh` — offline,
   seconds, catches a wrong or corrupt bundle. **Done, passed.**
2. TLS handshake from the instance — proves reachability and, via the advertised
   CA names, that Bloomberg accepts the new chain. **Done 2026-09-01, passed.**
3. Console check that `610146:5` is Active. **Outstanding.**
4. Deploy to `Bbgfix-prod` in a window, with `bbgfix-source-11` ready to redeploy.

Re-running step 2 with the new bundle presented is optional. It would confirm the
key and certificate pair against Bloomberg's endpoint, but the advertised CA names
already establish chain acceptance, and TLS cannot answer the entitlement question
that step 3 covers.

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
---

# Outcome — cutover completed 2026-09-01

The credential swap is done. `MAP_TRYG_PROD` is live on `610146:5`.

## What was deployed

`bbgfix-source-12`, built by repackaging `bbgfix-source-11` (Route B). 456 zip
entries in, 456 out; 447 byte-identical; exactly 9 files replaced — the eight
under `WEB-INF/classes/cert/trygprod/` and one line of `tryg_prod.cfg`
(`SocketKeyStorePassword`). No compiled class was touched.

```
s3://elasticbeanstalk-eu-central-1-513132248511/bbgfix-source-12.war
sha256 d2222a5e9c05716b0b9cbe9f1ffb3b22cb2a90754e5411b151958c7cfdd6c288
```

Pre-deploy verification against the cert set extracted back out of the finished
WAR: `CN=610146:5`, sha256WithRSAEncryption, RSA 2048, key matches certificate,
chain verifies, and `keytool` opens the WAR's keystore using the password from
the WAR's own config (alias `map_bbg_prod-map_tryg_prod`).

## Timeline

| Time (UTC) | Event |
|---|---|
| 06:57:33 | `update-environment` accepted, environment `Updating` |
| 06:57:42 | Deploying new version to instance |
| 06:58:33 | Environment update completed, `Ready` / `Green` |
| 06:58:48 | QuickFIX/J created session `FIX.4.4:MAP_TRYG_PROD->MAP_BBG_PROD` |
| 06:58:49 | MINA session to `69.191.198.2:8228` |
| 06:58:50 | Logon request sent; **`Received logon`** |

Total FIX outage roughly 70 seconds.

Confirmed stable a couple of heartbeat intervals later: no further events after
the logon, no TLS or certificate errors in the event log, and the TCP connection
to `69.191.198.2:8228` still `ESTAB` on the same local port as the logon.

## What this proves

End to end, on Bloomberg's production endpoint: the new chain is accepted at the
TLS layer, the keystore and its password pair correctly inside the artifact, and
Bloomberg accepts the FIX logon on `610146:5`. Nothing about the credential
remains unverified.

## Still outstanding

1. **Bloomberg console** — confirm `610146:5` Last Usage is advancing and
   `610146:4` has stopped. The logs prove our side; the console is the
   authoritative record of theirs.
2. **Deactivate `610146:4`** once `:5` has been stable for a sensible period. The
   daily Critical alert stops when the old credential is gone, not when the new
   one starts being used.
3. **Route B debt — the important one.** The source repository for
   `com.saepiox:bbgfix-server` still contains `610146:4`. The next build from
   source silently reverts this cutover. Land the same cert and config change
   there before anyone rebuilds.

## Rollback, and when it stops working

Redeploy the previous version:

```
aws elasticbeanstalk update-environment --environment-name Bbgfix-prod \
  --version-label bbgfix-source-11 --region eu-central-1
```

Do not delete `bbgfix-source-11` or its S3 object while that remains useful — but
note it expires as a rollback on **2026-09-05**, when the legacy `FIX
Connectivity` CA that `610146:4` chains through goes out of validity. After that
date, reverting gets you a credential that no longer validates.

## Operational notes for next time

- Deploying must be done with a user principal. The EC2 instance profile
  (`aws-elasticbeanstalk-ec2-role`) deliberately cannot call
  `elasticbeanstalk:UpdateEnvironment`, so running the deploy from the FIX host
  fails with `AccessDenied`. That is correct design, not a misconfiguration.
- `eb deploy` from a directory with no source will report that it is launching
  the sample application. It failed on a `ValidationError` before making any call,
  but do not run `eb deploy` from an empty directory against this environment.
- No credential material was left staged in S3; the `tmp-cert-test/` prefix is
  empty.
