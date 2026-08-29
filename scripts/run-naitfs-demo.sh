#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
PAYMENT_SERVICE_URL="${PAYMENT_SERVICE_URL:-http://localhost:8082}"
COMPLIANCE_SERVICE_URL="${COMPLIANCE_SERVICE_URL:-http://localhost:8084}"
KNOWLEDGE_SERVICE_URL="${KNOWLEDGE_SERVICE_URL:-http://localhost:8088}"
DOCUMENT_SERVICE_URL="${DOCUMENT_SERVICE_URL:-http://localhost:8089}"
DASHBOARD_PATH="$ROOT_DIR/dashboard/index.html"
DASHBOARD_URL="${DASHBOARD_URL:-http://127.0.0.1:4173/dashboard/}"
SUMMARY_FILE="$ROOT_DIR/target/naitfs-demo-summary.txt"
MVP11_LOG="$ROOT_DIR/target/mvp15-regulated-trust-flow.log"
MVP12_LOG="$ROOT_DIR/target/mvp15-postman.log"
HEALTH_WAIT_SECONDS="${HEALTH_WAIT_SECONDS:-180}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-3}"

TENANT_ID="unavailable"
CORRELATION_ID="unavailable"
EVIDENCE_COUNT="unavailable"
POSTMAN_STATUS="SKIPPED"
DASHBOARD_STATUS="NOT_RUN"
MVP13_STATUS="NOT_RUN"

section() {
  echo
  echo "============================================================"
  echo " $1"
  echo "============================================================"
}

fail() {
  echo "FAIL: $1"
  write_summary "FAILED: $1"
  exit 1
}

pass() {
  echo "PASS: $1"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail "Required command not found: $1"
  fi
  pass "$1 is available"
}

optional_cmd() {
  if command -v "$1" >/dev/null 2>&1; then
    pass "$1 is available"
    return 0
  fi

  echo "WARN: $1 is not installed; related validation will be skipped."
  return 1
}

write_summary() {
  local result="${1:-READY}"
  mkdir -p "$ROOT_DIR/target"
  cat > "$SUMMARY_FILE" <<SUMMARY
NAITFS One-Command Demo Summary
Generated At: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
Result: $result

Gateway URL: $GATEWAY_URL
Dashboard Path: $DASHBOARD_PATH
Dashboard URL: $DASHBOARD_URL
Tenant ID Used: $TENANT_ID
Correlation ID: $CORRELATION_ID
Evidence Count: $EVIDENCE_COUNT
Postman Validation Status: $POSTMAN_STATUS
MVP13 Validation Status: $MVP13_STATUS
Dashboard Validation Status: $DASHBOARD_STATUS

Logs:
- MVP11 regulated trust flow: $MVP11_LOG
- MVP12 Postman validation: $MVP12_LOG
SUMMARY
}

wait_for_health() {
  local name="$1"
  local url="$2"
  local deadline=$((SECONDS + HEALTH_WAIT_SECONDS))
  local body=""

  echo "Waiting for $name at $url"
  while [ "$SECONDS" -le "$deadline" ]; do
    body="$(curl -sS "$url" 2>/dev/null || true)"
    if echo "$body" | jq -e '.status == "UP"' >/dev/null 2>&1; then
      pass "$name is UP"
      return 0
    fi

    sleep "$POLL_INTERVAL_SECONDS"
  done

  echo "$body" | jq . 2>/dev/null || echo "$body"
  fail "$name did not report UP at $url"
}

run_script() {
  local script_path="$1"
  shift

  if [ ! -x "$script_path" ]; then
    fail "Required script is missing or not executable: $script_path"
  fi

  "$script_path" "$@"
}

extract_demo_values() {
  TENANT_ID="$(awk -F': *' '/Tenant ID:/ {value=$2} END {print value}' "$MVP11_LOG")"
  CORRELATION_ID="$(awk -F': *' '/Correlation ID:/ {value=$2} END {print value}' "$MVP11_LOG")"
  EVIDENCE_COUNT="$(awk -F': *' '/Evidence Count:/ {value=$2} END {print value}' "$MVP11_LOG")"

  TENANT_ID="${TENANT_ID:-unavailable}"
  CORRELATION_ID="${CORRELATION_ID:-unavailable}"
  EVIDENCE_COUNT="${EVIDENCE_COUNT:-unavailable}"
}

obtain_token() {
  if [ -n "${TOKEN:-}" ]; then
    echo "$TOKEN"
    return 0
  fi

  if [ -x "$ROOT_DIR/scripts/get-token.sh" ]; then
    "$ROOT_DIR/scripts/get-token.sh" >/dev/null
    if [ -s "$ROOT_DIR/.token" ]; then
      cat "$ROOT_DIR/.token"
      return 0
    fi
  fi

  fail "Unable to obtain a JWT token for gateway validation"
}

validate_mvp13() {
  local token="$1"
  local summary_file dashboard_file summary_status dashboard_status

  if [ "$TENANT_ID" = "unavailable" ] || [ "$CORRELATION_ID" = "unavailable" ]; then
    fail "Cannot validate MVP13 because tenant or correlation ID was not captured"
  fi

  summary_file="$(mktemp "${TMPDIR:-/tmp}/mvp15-summary.XXXXXX")"
  dashboard_file="$(mktemp "${TMPDIR:-/tmp}/mvp15-dashboard.XXXXXX")"

  summary_status="$(curl -sS -o "$summary_file" -w "%{http_code}" \
    "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID/summary" \
    -H "Authorization: Bearer $token" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "Accept: application/json" || true)"

  if [ "$summary_status" != "200" ]; then
    cat "$summary_file" 2>/dev/null || true
    rm -f "$summary_file" "$dashboard_file"
    fail "MVP13 correlation summary returned HTTP $summary_status"
  fi

  jq -e '
    .tenantId
    and .correlationId
    and (.totalEvidence | type == "number")
    and (.evidenceTypes | type == "array")
    and (.sourceServices | type == "array")
    and .status
  ' "$summary_file" >/dev/null || {
    cat "$summary_file" | jq . 2>/dev/null || cat "$summary_file"
    rm -f "$summary_file" "$dashboard_file"
    fail "MVP13 correlation summary response shape is invalid"
  }

  dashboard_status="$(curl -sS -o "$dashboard_file" -w "%{http_code}" \
    "$GATEWAY_URL/api/v1/evidence/demo-dashboard?tenantId=$TENANT_ID" \
    -H "Authorization: Bearer $token" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "Accept: application/json" || true)"

  if [ "$dashboard_status" != "200" ]; then
    cat "$dashboard_file" 2>/dev/null || true
    rm -f "$summary_file" "$dashboard_file"
    fail "MVP13 demo-dashboard returned HTTP $dashboard_status"
  fi

  jq -e '
    .tenantId
    and .totals
    and (.evidenceByType | type == "object")
    and (.recentTrustFlows | type == "array")
    and .readinessStatus
  ' "$dashboard_file" >/dev/null || {
    cat "$dashboard_file" | jq . 2>/dev/null || cat "$dashboard_file"
    rm -f "$summary_file" "$dashboard_file"
    fail "MVP13 demo-dashboard response shape is invalid"
  }

  rm -f "$summary_file" "$dashboard_file"
  MVP13_STATUS="PASSED"
  pass "MVP13 gateway endpoints returned compatible JSON"
}

validate_mvp14() {
  local token="$1"

  if [ -x "$ROOT_DIR/scripts/validate-mvp14-dashboard.sh" ]; then
    if ! TOKEN="$token" TENANT_ID="$TENANT_ID" GATEWAY_URL="$GATEWAY_URL" DASHBOARD_URL="$DASHBOARD_URL" \
      "$ROOT_DIR/scripts/validate-mvp14-dashboard.sh"; then
      DASHBOARD_STATUS="FAILED"
      fail "MVP14 dashboard validation failed"
    fi
  else
    [ -f "$ROOT_DIR/dashboard/index.html" ] || fail "Missing dashboard/index.html"
    [ -f "$ROOT_DIR/dashboard/styles.css" ] || fail "Missing dashboard/styles.css"
    [ -f "$ROOT_DIR/dashboard/app.js" ] || fail "Missing dashboard/app.js"
    grep -q "/api/v1/evidence/demo-dashboard" "$ROOT_DIR/dashboard/app.js" \
      || fail "MVP14 dashboard does not call demo-dashboard endpoint"
  fi

  DASHBOARD_STATUS="PASSED"
  pass "MVP14 dashboard validation passed"
}

section "NAITFS MVP15 One-Command Demo"
echo "Gateway URL:    $GATEWAY_URL"
echo "Dashboard Path: $DASHBOARD_PATH"
mkdir -p "$ROOT_DIR/target"

section "Checking Required Tools"
require_cmd docker
require_cmd curl
require_cmd jq
require_cmd mvn
require_cmd java
if optional_cmd newman; then
  POSTMAN_STATUS="AVAILABLE"
else
  POSTMAN_STATUS="SKIPPED: newman not installed"
fi

section "Starting NAITFS Platform"
if ! run_script "$ROOT_DIR/scripts/start-naitfs-platform.sh"; then
  fail "NAITFS platform startup failed"
fi

section "Repairing Keycloak Demo Configuration"
if [ -x "$ROOT_DIR/scripts/repair-keycloak-demo.sh" ]; then
  if ! "$ROOT_DIR/scripts/repair-keycloak-demo.sh"; then
    fail "Keycloak demo repair failed"
  fi
elif [ -x "$ROOT_DIR/scripts/repair-keycloak-test-user.sh" ]; then
  if ! "$ROOT_DIR/scripts/repair-keycloak-test-user.sh"; then
    fail "Keycloak test user repair failed"
  fi
else
  echo "WARN: No Keycloak repair/setup script found; continuing after platform startup."
fi

section "Waiting For Service Health"
wait_for_health "API Gateway" "$GATEWAY_URL/actuator/health"
wait_for_health "Payment Orchestration Service" "$PAYMENT_SERVICE_URL/actuator/health"
wait_for_health "Compliance Evidence Service" "$COMPLIANCE_SERVICE_URL/actuator/health"
wait_for_health "Knowledge Service" "$KNOWLEDGE_SERVICE_URL/actuator/health"
wait_for_health "Document Intelligence Service" "$DOCUMENT_SERVICE_URL/actuator/health"

section "Resetting Demo Data"
if ! run_script "$ROOT_DIR/scripts/reset-demo-data.sh"; then
  fail "Demo data reset failed"
fi

section "Running MVP11 Regulated Trust Flow"
if ! run_script "$ROOT_DIR/scripts/demo-regulated-trust-flow.sh" | tee "$MVP11_LOG"; then
  fail "MVP11 regulated trust flow failed"
fi
extract_demo_values
pass "Captured tenant=$TENANT_ID correlation=$CORRELATION_ID evidenceCount=$EVIDENCE_COUNT"

section "Running MVP12 External API Validation"
if command -v newman >/dev/null 2>&1; then
  if "$ROOT_DIR/scripts/run-mvp12-postman-tests.sh" | tee "$MVP12_LOG"; then
    POSTMAN_STATUS="PASSED"
  else
    POSTMAN_STATUS="FAILED"
    fail "MVP12 Postman validation failed"
  fi
else
  echo "SKIP: newman is not installed."
  POSTMAN_STATUS="SKIPPED: newman not installed"
fi

section "Obtaining Gateway Token"
TOKEN_VALUE="$(obtain_token)"
pass "Gateway token obtained"

section "Validating MVP13 Endpoint Shape"
validate_mvp13 "$TOKEN_VALUE"

section "Validating MVP14 Dashboard"
validate_mvp14 "$TOKEN_VALUE"

write_summary "READY"

section "NAITFS Demo Summary"
cat "$SUMMARY_FILE"

echo
echo "READY FOR ONE-COMMAND NAITFS DEMO"
