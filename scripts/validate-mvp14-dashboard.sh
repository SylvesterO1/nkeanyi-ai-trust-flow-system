#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TENANT_ID="${TENANT_ID:-tenant-mvp13-demo}"
DASHBOARD_URL="${DASHBOARD_URL:-http://127.0.0.1:4173/dashboard/}"
ENDPOINT="$GATEWAY_URL/api/v1/evidence/demo-dashboard?tenantId=$TENANT_ID"

fail() {
  echo "FAIL: $1"
  exit 1
}

pass() {
  echo "PASS: $1"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail "Required command not found: $1"
  fi
}

require_file() {
  if [ ! -f "$1" ]; then
    fail "Missing required dashboard file: $1"
  fi
  pass "Found $1"
}

require_cmd curl
require_cmd jq

require_file "dashboard/index.html"
require_file "dashboard/styles.css"
require_file "dashboard/app.js"
require_file "docs/mvp14-demo-dashboard-ui-guide.md"

grep -q "Nkeanyi-AI Trust Flow System" dashboard/index.html \
  || fail "Dashboard does not show the platform name"
grep -q "/api/v1/evidence/demo-dashboard" dashboard/app.js \
  || fail "Dashboard does not call the MVP13 demo-dashboard endpoint"
grep -q "http://localhost:8080" dashboard/app.js \
  || fail "Dashboard does not default gatewayUrl to http://localhost:8080"

HEALTH_STATUS="$(curl -sS -o /dev/null -w "%{http_code}" "$GATEWAY_URL/actuator/health" || true)"
if [ "$HEALTH_STATUS" != "200" ]; then
  fail "API Gateway is not reachable at $GATEWAY_URL/actuator/health (HTTP $HEALTH_STATUS)"
fi
pass "API Gateway is reachable"

if [ -z "${TOKEN:-}" ]; then
  fail "TOKEN is required. Export a JWT with evidence.read scope before validating MVP14."
fi

RESPONSE_FILE="$(mktemp "${TMPDIR:-/tmp}/mvp14-dashboard.XXXXXX")"
cleanup() {
  rm -f "$RESPONSE_FILE"
}
trap cleanup EXIT

HTTP_STATUS="$(curl -sS -o "$RESPONSE_FILE" -w "%{http_code}" \
  "$ENDPOINT" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "Accept: application/json" || true)"

if [ "$HTTP_STATUS" != "200" ]; then
  cat "$RESPONSE_FILE" 2>/dev/null || true
  fail "demo-dashboard endpoint returned HTTP $HTTP_STATUS"
fi

jq -e '
  .tenantId
  and .serviceName
  and .generatedAt
  and .totals
  and (.totals.totalEvidence | type == "number")
  and (.totals.totalCorrelations | type == "number")
  and (.evidenceByType | type == "object")
  and (.recentTrustFlows | type == "array")
  and .readinessStatus
' "$RESPONSE_FILE" >/dev/null || fail "demo-dashboard response is not valid MVP13 dashboard JSON"

pass "demo-dashboard endpoint returned valid JSON"
echo "Dashboard URL: $DASHBOARD_URL"
echo "Dashboard file: $ROOT_DIR/dashboard/index.html"
echo "READY FOR EXECUTIVE DASHBOARD DEMO"
