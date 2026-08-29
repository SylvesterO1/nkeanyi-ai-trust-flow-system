#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TENANT_ID="${TENANT_ID:-tenant-demo-001}"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required but not installed."
  exit 1
fi

if [ ! -f ".token" ] || [ ! -s ".token" ]; then
  echo "Token file missing or empty. Generating a fresh token..."
  ./scripts/get-token.sh >/dev/null
fi

TOKEN="$(cat .token)"

TOKEN_EXP=$(echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r '.exp // 0' 2>/dev/null || echo 0)
NOW=$(date +%s)

if [ "$TOKEN_EXP" -le "$NOW" ]; then
  echo "Token expired. Generating a fresh token..."
  ./scripts/get-token.sh >/dev/null
  TOKEN="$(cat .token)"
fi

IDEMPOTENCY_KEY="idem-$(uuidgen)"
CORRELATION_ID="corr-$(uuidgen)"

CUSTOMER_ID="${CUSTOMER_ID:-customer-demo-$(date +%s)}"
AMOUNT="${AMOUNT:-2000000.99}"
CURRENCY="${CURRENCY:-USD}"
SOURCE_ACCOUNT="${SOURCE_ACCOUNT:-ACC-SOURCE-$(date +%s)}"
DESTINATION_ACCOUNT="${DESTINATION_ACCOUNT:-ACC-DEST-$(date +%s)}"

REQUEST_BODY=$(jq -n \
  --arg customerId "$CUSTOMER_ID" \
  --arg currency "$CURRENCY" \
  --arg sourceAccount "$SOURCE_ACCOUNT" \
  --arg destinationAccount "$DESTINATION_ACCOUNT" \
  --argjson amount "$AMOUNT" \
  '{
    customerId: $customerId,
    amount: $amount,
    currency: $currency,
    sourceAccount: $sourceAccount,
    destinationAccount: $destinationAccount
  }'
)

TMP_RESPONSE="$(mktemp)"
TMP_HEADERS="$(mktemp)"

echo "=================================================="
echo " Creating N-AITFS payment through API Gateway"
echo "=================================================="
echo "Gateway URL:      $GATEWAY_URL"
echo "Tenant ID:        $TENANT_ID"
echo "Correlation ID:   $CORRELATION_ID"
echo "Idempotency Key:  $IDEMPOTENCY_KEY"
echo "Customer ID:      $CUSTOMER_ID"
echo "Amount:           $AMOUNT $CURRENCY"
echo "=================================================="
echo ""

HTTP_STATUS=$(curl -s -o "$TMP_RESPONSE" -D "$TMP_HEADERS" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "$REQUEST_BODY")

echo "HTTP Status: $HTTP_STATUS"
echo ""

if [ "$HTTP_STATUS" != "201" ] && [ "$HTTP_STATUS" != "200" ]; then
  echo "Payment creation FAILED."
  echo ""
  echo "Response headers:"
  cat "$TMP_HEADERS"
  echo ""
  echo "Response body:"
  cat "$TMP_RESPONSE" | jq . 2>/dev/null || cat "$TMP_RESPONSE"
  rm -f "$TMP_RESPONSE" "$TMP_HEADERS"
  exit 1
fi

PAYMENT_ID=$(jq -r '.paymentId // empty' "$TMP_RESPONSE")
PAYMENT_STATUS=$(jq -r '.status // empty' "$TMP_RESPONSE")
DB_ID=$(jq -r '.id // empty' "$TMP_RESPONSE")

echo "Payment creation SUCCEEDED."
echo ""
echo "Payment result:"
cat "$TMP_RESPONSE" | jq .
echo ""
echo "Summary:"
echo "  DB ID:           $DB_ID"
echo "  Payment ID:      $PAYMENT_ID"
echo "  Status:          $PAYMENT_STATUS"
echo "  Tenant ID:       $TENANT_ID"
echo "  Correlation ID:  $CORRELATION_ID"
echo "  Idempotency Key: $IDEMPOTENCY_KEY"
echo ""

mkdir -p target/demo-results

RESULT_FILE="target/demo-results/latest-payment.json"

jq -n \
  --arg paymentId "$PAYMENT_ID" \
  --arg status "$PAYMENT_STATUS" \
  --arg tenantId "$TENANT_ID" \
  --arg correlationId "$CORRELATION_ID" \
  --arg idempotencyKey "$IDEMPOTENCY_KEY" \
  --arg httpStatus "$HTTP_STATUS" \
  --slurpfile response "$TMP_RESPONSE" \
  '{
    paymentId: $paymentId,
    status: $status,
    tenantId: $tenantId,
    correlationId: $correlationId,
    idempotencyKey: $idempotencyKey,
    httpStatus: $httpStatus,
    response: $response[0]
  }' > "$RESULT_FILE"

echo "Saved latest payment result to:"
echo "  $RESULT_FILE"

rm -f "$TMP_RESPONSE" "$TMP_HEADERS"
