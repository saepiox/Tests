#!/usr/bin/env python3
"""
Local synthetic SaepioX stand-in.

This is NOT the real SaepioX application. It is a lightweight test double that
reproduces the login / UI contract the Selenium harness (src/main/java/tester)
drives, seeded with SYNTHETIC data only. Its purpose is to give a fully local,
end-to-end runnable test environment on this machine so the new Tryg integration
flow can be developed and tested without touching production or real data.

Run:  python3 testenv/server.py [--host 127.0.0.1] [--port 5000]
Then point the harness at  http://127.0.0.1:5000/

Everything is in-memory and resets on restart. No external services, no real
customer data, no persistence.
"""

import argparse
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

STATIC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static")

# ---------------------------------------------------------------------------
# Synthetic data (test doubles only — no production data)
# ---------------------------------------------------------------------------

# Test accounts. Passwords are throwaway test values, never real credentials.
USERS = {
    # Internal SaepioX QA admin
    "tj@saepiox.com": {
        "password": "pass",
        "displayName": "TJ (QA Admin)",
        "role": "ADMIN",
        "source": "SAEPIOX",
    },
    # Dedicated Tryg integration test login (synthetic)
    "tryg-tester": {
        "password": "tryg-test-01",
        "displayName": "Tryg Integration Tester",
        "role": "PARTNER",
        "source": "TRYG",
    },
}

# Synthetic agreements shown on the dashboard.
AGREEMENTS = [
    {"name": "charles", "type": "ASSET_MANAGER", "currency": "DKK", "risk": 3, "term": 5},
    {"name": "nordic-fund", "type": "ASSET_MANAGER", "currency": "DKK", "risk": 2, "term": 7},
]

# Synthetic Tryg-sourced integration records. In the real integration these
# would arrive from Tryg's systems; here they are fixed test fixtures.
TRYG_POLICIES = [
    {"policyId": "TRYG-1001", "product": "Erhverv Ansvar", "currency": "DKK", "premium": 12500, "status": "ACTIVE"},
    {"policyId": "TRYG-1002", "product": "Bygning", "currency": "DKK", "premium": 30400, "status": "ACTIVE"},
    {"policyId": "TRYG-1003", "product": "Transport", "currency": "DKK", "premium": 8800, "status": "PENDING"},
]


class Handler(BaseHTTPRequestHandler):
    server_version = "SaepioXStandin/0.1"

    # -- helpers ------------------------------------------------------------
    def _send(self, code, body=b"", content_type="text/plain; charset=utf-8"):
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def _json(self, code, obj):
        self._send(code, json.dumps(obj), "application/json; charset=utf-8")

    def _read_json(self):
        length = int(self.headers.get("Content-Length") or 0)
        if not length:
            return {}
        try:
            return json.loads(self.rfile.read(length) or b"{}")
        except (ValueError, TypeError):
            return {}

    def _serve_static(self, rel):
        rel = rel.lstrip("/")
        if rel in ("", "index.html"):
            rel = "index.html"
        path = os.path.normpath(os.path.join(STATIC_DIR, rel))
        if not path.startswith(STATIC_DIR) or not os.path.isfile(path):
            # SPA fallback: any unknown non-api path serves index.html
            path = os.path.join(STATIC_DIR, "index.html")
        ctype = "text/html; charset=utf-8"
        if path.endswith(".js"):
            ctype = "application/javascript; charset=utf-8"
        elif path.endswith(".css"):
            ctype = "text/css; charset=utf-8"
        with open(path, "rb") as fh:
            self._send(200, fh.read(), ctype)

    # -- routing ------------------------------------------------------------
    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/api/health":
            return self._json(200, {"status": "ok", "app": "saepiox-standin", "synthetic": True})
        if path == "/api/agreements":
            return self._json(200, {"agreements": AGREEMENTS})
        if path == "/api/tryg/policies":
            return self._json(200, {"source": "TRYG", "synthetic": True, "policies": TRYG_POLICIES})
        if path.startswith("/api/"):
            return self._json(404, {"error": "unknown endpoint"})
        return self._serve_static(path)

    def do_POST(self):
        path = urlparse(self.path).path
        if path == "/api/login":
            data = self._read_json()
            login = (data.get("login") or "").strip()
            pw = data.get("password") or ""
            user = USERS.get(login)
            if user and user["password"] == pw:
                return self._json(200, {
                    "ok": True,
                    "displayName": user["displayName"],
                    "role": user["role"],
                    "source": user["source"],
                })
            return self._json(401, {"ok": False, "error": "invalid credentials"})
        return self._json(404, {"error": "unknown endpoint"})

    def log_message(self, fmt, *args):  # keep test output clean
        pass


def main():
    ap = argparse.ArgumentParser(description="Local synthetic SaepioX stand-in")
    ap.add_argument("--host", default=os.environ.get("STANDIN_HOST", "127.0.0.1"))
    ap.add_argument("--port", type=int, default=int(os.environ.get("STANDIN_PORT", "5000")))
    args = ap.parse_args()
    httpd = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"SaepioX stand-in (SYNTHETIC) listening on http://{args.host}:{args.port}/")
    print("  admin login: tj@saepiox.com / pass")
    print("  tryg  login: tryg-tester / tryg-test-01")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        httpd.shutdown()


if __name__ == "__main__":
    main()
