#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-naitfs-client}"
USERNAME="${USERNAME:-test-user}"
PASSWORD="${PASSWORD:-test-password}"
EVIDENCE_WAIT_SECONDS="${EVIDENCE_WAIT_SECONDS:-20}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-2}"

echo "=================================================="
echo " N-AITFS MVP 7 Smoke Test"
echo "=================================================="
echo "Root: $ROOT_DIR"
echo "Gateway URL: $GATEWAY_URL"
echo ""

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "FAIL: Required command not found: $cmd"
    exit 1
  fi
}

pass() {
  echo "PASS: $1"
}

fail() {
  echo "FAIL: $1"
  exit 1
}

assert_json_count_gt_zero() {
  local response="$1"
  local label="$2"
  local count

  count="$(echo "$response" | jq -r '.count // 0')"
  if [ "$count" -gt 0 ]; then
    pass "$label returned evidence count=$count"
    return
  fi

  echo "$response" | jq .
  fail "$label returned no evidence"
}

assert_evidence_fields() {
  local response="$1"
  local label="$2"

  local tenant_match
  local payment_match
  local decision_count
  local risk_level_count

  tenant_match="$(echo "$response" | jq -r --arg tenantId "$TENANT_ID" '[.evidence[]? | select(.tenantId == $tenantId)] | length')"
  payment_match="$(echo "$response" | jq -r --arg paymentId "$PAYMENT_ID" '[.evidence[]? | select(.paymentId == $paymentId)] | length')"
  decision_count="$(echo "$response" | jq -r '[.evidence[]? | select((.decision // "") != "")] | length')"
  risk_level_count="$(echo "$response" | jq -r '[.evidence[]? | select((.riskLevel // "") != "")] | length')"

  if [ "$tenant_match" -lt 1 ]; then
    echo "$response" | jq .
    fail "$label did not include tenantId=$TENANT_ID"
  fi
  pass "$label includes tenantId=$TENANT_ID"

  if [ "$payment_match" -lt 1 ]; then
    echo "$response" | jq .
    fail "$label did not include paymentId=$PAYMENT_ID"
  fi
  pass "$label includes paymentId=$PAYMENT_ID"

  if [ "$decision_count" -lt 1 ]; then
    echo "$response" | jq .
    fail "$label did not include a decision"
  fi
  pass "$label includes decision"

  if [ "$risk_level_count" -lt 1 ]; then
    echo "$response" | jq .
    fail "$label did not include a riskLevel"
  fi
  pass "$label includes riskLevel"
}

require_cmd curl
require_cmd jq
require_cmd uuidgen

echo "Step 1: Generating JWT token from local Keycloak..."
echo ""

TOKEN_RESPONSE="$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" \
  -d "scope=openid profile email payments.write payments.read evidence.read")"

TOKEN="$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')"

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "$TOKEN_RESPONSE" | jq . 2>/dev/null || echo "$TOKEN_RESPONSE"
  fail "Failed to generate JWT token"
fi

pass "JWT token generated"
echo ""

echo "Step 2: Creating unique tenant and correlation IDs..."
echo ""

TENANT_ID="tenant-mvp7-$(uuidgen | tr '[:upper:]' '[:lower:]' | cut -c1-8)"
WRONG_TENANT_ID="tenant-wrong-$(uuidgen | tr '[:upper:]' '[:lower:]' | cut -c1-8)"
CORRELATION_ID="corr-$(uuidgen | tr '[:upper:]' '[:lower:]')"
IDEMPOTENCY_KEY="idem-mvp7-$(uuidgen | tr '[:upper:]' '[:lower:]')"
CUSTOMER_ID="customer-mvp7-$(date +%s)"
AMOUNT="${AMOUNT:-2000000.99}"
CURRENCY="${CURRENCY:-USD}"

echo "Tenant ID:        $TENANT_ID"
echo "Wrong Tenant ID:  $WRONG_TENANT_ID"
echo "Correlation ID:   $CORRELATION_ID"
echo "Idempotency Key:  $IDEMPOTENCY_KEY"
echo ""

echo "Step 3: Posting payment through API Gateway..."
echo ""

REQUEST_BODY="$(jq -n \
  --arg customerId "$CUSTOMER_ID" \
  --arg sourceAccount "ACC-SOURCE-MVP7" \
  --arg destinationAccount "ACC-DEST-MVP7" \
  --arg currency "$CURRENCY" \
  --argjson amount "$AMOUNT" \
  '{
    customerId: $customerId,
    sourceAccount: $sourceAccount,
    destinationAccount: $destinationAccount,
    amount: $amount,
    currency: $currency
  }')"

TMP_PAYMENT_RESPONSE="$(mktemp)"
PAYMENT_HTTP_STATUS="$(curl -s -o "$TMP_PAYMENT_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "$REQUEST_BODY")"

if [ "$PAYMENT_HTTP_STATUS" != "201" ] && [ "$PAYMENT_HTTP_STATUS" != "200" ]; then
  cat "$TMP_PAYMENT_RESPONSE" | jq . 2>/dev/null || cat "$TMP_PAYMENT_RESPONSE"
  rm -f "$TMP_PAYMENT_RESPONSE"
  fail "Payment POST returned HTTP $PAYMENT_HTTP_STATUS"
fi

PAYMENT_ID="$(jq -r '.paymentId // empty' "$TMP_PAYMENT_RESPONSE")"

if [ -z "$PAYMENT_ID" ] || [ "$PAYMENT_ID" = "null" ]; then
  cat "$TMP_PAYMENT_RESPONSE" | jq .
  rm -f "$TMP_PAYMENT_RESPONSE"
  fail "paymentId missing from payment response"
fi

rm -f "$TMP_PAYMENT_RESPONSE"
pass "Payment created with paymentId=$PAYMENT_ID"
echo ""

echo "Step 4: Waiting for Kafka and evidence processing..."
echo ""

PAYMENT_EVIDENCE_RESPONSE=""
CORRELATION_EVIDENCE_RESPONSE=""
deadline=$((SECONDS + EVIDENCE_WAIT_SECONDS))

while [ "$SECONDS" -le "$deadline" ]; do
  PAYMENT_EVIDENCE_RESPONSE="$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    "$GATEWAY_URL/api/v1/evidence/payment/$PAYMENT_ID")"

  CORRELATION_EVIDENCE_RESPONSE="$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID")"

  payment_count="$(echo "$PAYMENT_EVIDENCE_RESPONSE" | jq -r '.count // 0')"
  correlation_count="$(echo "$CORRELATION_EVIDENCE_RESPONSE" | jq -r '.count // 0')"

  if [ "$payment_count" -gt 0 ] && [ "$correlation_count" -gt 0 ]; then
    break
  fi

  sleep "$POLL_INTERVAL_SECONDS"
done

echo "Step 5: Validating evidence by paymentId through API Gateway..."
echo ""

assert_json_count_gt_zero "$PAYMENT_EVIDENCE_RESPONSE" "Payment evidence query"
assert_evidence_fields "$PAYMENT_EVIDENCE_RESPONSE" "Payment evidence query"
echo ""

echo "Step 6: Validating evidence by correlationId through API Gateway..."
echo ""

assert_json_count_gt_zero "$CORRELATION_EVIDENCE_RESPONSE" "Correlation evidence query"
assert_evidence_fields "$CORRELATION_EVIDENCE_RESPONSE" "Correlation evidence query"
echo ""

echo "Step 7: Validating tenant isolation with wrong tenant..."
echo ""

WRONG_TENANT_RESPONSE="$(curl -s \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $WRONG_TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  "$GATEWAY_URL/api/v1/evidence/payment/$PAYMENT_ID")"

WRONG_TENANT_COUNT="$(echo "$WRONG_TENANT_RESPONSE" | jq -r '.count // 0')"
WRONG_TENANT_EVIDENCE_LENGTH="$(echo "$WRONG_TENANT_RESPONSE" | jq -r '.evidence // [] | length')"

if [ "$WRONG_TENANT_COUNT" -eq 0 ] || [ "$WRONG_TENANT_EVIDENCE_LENGTH" -eq 0 ]; then
  pass "Wrong tenant returned no evidence"
else
  echo "$WRONG_TENANT_RESPONSE" | jq .
  fail "Wrong tenant unexpectedly received evidence"
fi

echo ""
echo "=================================================="
echo " MVP 7 SMOKE TEST PASSED"
echo "=================================================="
echo "Tenant ID:       $TENANT_ID"
echo "Correlation ID:  $CORRELATION_ID"
echo "Payment ID:      $PAYMENT_ID"
echo "Wrong Tenant ID: $WRONG_TENANT_ID"
echo "=================================================="
