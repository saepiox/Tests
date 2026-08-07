#!/usr/bin/env bash
#
# Run the Tryg integration test against the local synthetic SaepioX stand-in.
#
# Starts the stand-in server (unless one is already running on the host), runs
# the Selenium test headless against it, then stops the server it started.
#
# Machine-specific bits (chromedriver + chromium paths) live here, not in the
# committed Java, so the harness itself stays portable. Override any of these
# via environment variables:
#
#   SAEPIOX_HOST     default http://127.0.0.1:5000/
#   CHROMEDRIVER     default <repo>/testenv/.drivers/chromedriver-linux64/chromedriver
#   CHROME_BINARY    default /opt/pw-browsers/chromium-1194/chrome-linux/chrome
#   TEST             default TrygIntegrationTest
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/.." && pwd)"

SAEPIOX_HOST="${SAEPIOX_HOST:-http://127.0.0.1:5000/}"
PORT="$(printf '%s' "$SAEPIOX_HOST" | sed -E 's#.*:([0-9]+).*#\1#')"
HOSTNAME_ONLY="127.0.0.1"
CHROMEDRIVER="${CHROMEDRIVER:-$HERE/.drivers/chromedriver-linux64/chromedriver}"
CHROME_BINARY="${CHROME_BINARY:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"
TEST="${TEST:-TrygIntegrationTest}"

started_server=0
cleanup() {
  if [[ "$started_server" == "1" && -n "${SERVER_PID:-}" ]]; then
    kill "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# Start the stand-in unless the host already answers.
if curl -sf "${SAEPIOX_HOST%/}/api/health" >/dev/null 2>&1; then
  echo "[run-tests] stand-in already running at $SAEPIOX_HOST"
else
  echo "[run-tests] starting stand-in on port $PORT"
  python3 "$HERE/server.py" --host "$HOSTNAME_ONLY" --port "$PORT" >"$HERE/.server.log" 2>&1 &
  SERVER_PID=$!
  started_server=1
  for _ in $(seq 1 30); do
    if curl -sf "${SAEPIOX_HOST%/}/api/health" >/dev/null 2>&1; then break; fi
    sleep 0.3
  done
  curl -sf "${SAEPIOX_HOST%/}/api/health" >/dev/null 2>&1 || { echo "[run-tests] stand-in failed to start"; cat "$HERE/.server.log"; exit 1; }
  echo "[run-tests] stand-in healthy"
fi

echo "[run-tests] running $TEST against $SAEPIOX_HOST"
cd "$REPO"
mvn -q -DfailIfNoTests=false \
  -Dtest="$TEST" \
  -Dsaepiox.host="$SAEPIOX_HOST" \
  -Dwebdriver.chrome.driver="$CHROMEDRIVER" \
  -Dchrome.binary="$CHROME_BINARY" \
  test
