# SaepioX local test environment (Tryg integration)

A **fully local** test environment for developing and running the Tryg
integration tests, with **no production data and no third-party exposure**.

> [!IMPORTANT]
> `testenv/` is **not** the real SaepioX application. It is a lightweight
> *synthetic stand-in* that reproduces the login/UI contract the Selenium
> harness drives, seeded with fixed test data. Its purpose is to let the
> harness run end-to-end on a developer machine. To test against the real app,
> point the harness at that host instead (see **Targeting another environment**).

## What's here

| Path | Purpose |
|------|---------|
| `server.py` | Zero-dependency Python stand-in server (SPA + JSON API), in-memory synthetic data |
| `static/` | The SaepioX-like SPA (login, dashboard, agreement, users, **Tryg** view) |
| `run-tests.sh` | Starts the stand-in and runs the Tryg Selenium test headless, then stops it |

The Selenium side lives in the normal project layout:

- `src/main/java/tester/DriverFactory.java` — portable headless-Chrome factory
- `src/main/java/tester/testingVariablesPile.java` — config from `-D`/env (host, admin + Tryg creds)
- `src/test/java/TrygIntegrationTest.java` — the Tryg integration test

## Test accounts (synthetic — not real credentials)

| Login | Password | Role |
|-------|----------|------|
| `tj@saepiox.com` | `pass` | SaepioX admin |
| `tryg-tester` | `tryg-test-01` | Tryg integration partner |

## Run it

Everything needed (JDK, Maven, Chromium, chromedriver) is expected on the
machine. One command starts the stand-in and runs the test:

```bash
./testenv/run-tests.sh
```

Expected result: `Tests run: 2, Failures: 0, Errors: 0`.

### Run the stand-in on its own

```bash
python3 testenv/server.py --host 127.0.0.1 --port 5000
# then open http://127.0.0.1:5000/  (or point the harness at it)
```

## How the pieces connect

```
run-tests.sh
  ├─ starts  testenv/server.py        →  http://127.0.0.1:5000/  (synthetic SaepioX)
  └─ runs    mvn -Dtest=TrygIntegrationTest
                 -Dsaepiox.host=…             ← which environment to hit
                 -Dwebdriver.chrome.driver=…  ← chromedriver 141 (matches Chromium)
                 -Dchrome.binary=…            ← Chromium binary
        →  DriverFactory.chrome()  →  headless Chromium  →  drives the SPA
```

## Targeting another environment

The harness reads its host and credentials from system properties (falling back
to env vars, then defaults) — no code edits needed to switch environments:

```bash
# Against a hosted staging instance instead of the local stand-in:
mvn -Dtest=TrygIntegrationTest \
    -Dsaepiox.host=https://staging.saepiox.com/ \
    -Dtryg.login=<user> -Dtryg.pass=<pass> \
    -Dwebdriver.chrome.driver=$PWD/testenv/.drivers/chromedriver-linux64/chromedriver \
    -Dchrome.binary=/opt/pw-browsers/chromium-1194/chrome-linux/chrome \
    test
```

| Property | Env var | Default |
|----------|---------|---------|
| `saepiox.host` | `SAEPIOX_HOST` | `http://127.0.0.1:5000/` |
| `saepiox.admin.login` | `SAEPIOX_ADMIN_LOGIN` | `tj@saepiox.com` |
| `saepiox.admin.pass` | `SAEPIOX_ADMIN_PASS` | `pass` |
| `tryg.login` | `TRYG_LOGIN` | `tryg-tester` |
| `tryg.pass` | `TRYG_PASS` | `tryg-test-01` |
| `chrome.binary` | `CHROME_BINARY` | (from `run-tests.sh`) |
| `chrome.headless` | — | `true` |

## Local-only artifacts (git-ignored)

`testenv/.drivers/` (the matching chromedriver) and `testenv/.server.log` are
generated locally and not committed. Fetch a chromedriver matching your
Chromium major version from
`https://storage.googleapis.com/chrome-for-testing-public/<version>/linux64/chromedriver-linux64.zip`.

## Notes / limitations

- The stand-in only implements the flows the tests exercise (login/logout,
  dashboard, agreement, users, Tryg). It is not a functional replica of SaepioX
  business logic.
- The legacy tests under `src/test/java` (`BrowserVerification`, etc.) target
  the **real** app's screens and hardcoded Windows paths; they are not wired
  into `run-tests.sh` and are not expected to pass against the stand-in.
