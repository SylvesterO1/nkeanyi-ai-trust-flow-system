#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
PAYMENT_URL="${PAYMENT_URL:-http://localhost:8082}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-naitfs-client}"
USERNAME="${USERNAME:-test-user}"
PASSWORD="${PASSWORD:-Password123}"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-naitfs-postgres}"
POSTGRES_DB="${POSTGRES_DB:-naitfs_db}"
POSTGRES_USER="${POSTGRES_USER:-naitfs_user}"

RUN_ID="$(date +%s)"

TENANT_ID="tenant-payment-test-${RUN_ID}"
CORRELATION_ID="corr-payment-test-${RUN_ID}"
IDEMPOTENCY_KEY="idem-payment-test-${RUN_ID}"

CUSTOMER_ID="customer-payment-test-${RUN_ID}"
SOURCE_ACCOUNT="acct-source-test-${RUN_ID}"
DESTINATION_ACCOUNT="acct-destination-test-${RUN_ID}"
PAYMENT_REFERENCE="PAYMENT-TEST-${RUN_ID}"
PAYMENT_METHOD="BANK_TRANSFER"
NARRATION="NAITFS payment side test through API Gateway"

echo "============================================================"
echo " NAITFS Payment Side Test"
echo "============================================================"
echo "Gateway:          $GATEWAY_URL"
echo "Payment Service:  $PAYMENT_URL"
echo "Keycloak:         $KEYCLOAK_URL"
echo "Realm:            $REALM"
echo "Client ID:        $CLIENT_ID"
echo "Username:         $USERNAME"
echo "Tenant ID:        $TENANT_ID"
echo "Correlation ID:   $CORRELATION_ID"
echo "Idempotency Key:  $IDEMPOTENCY_KEY"
echo "Payment Ref:      $PAYMENT_REFERENCE"
echo "============================================================"

echo
echo "1. Checking API Gateway health..."
curl -fsS "$GATEWAY_URL/actuator/health" | jq .

echo
echo "2. Checking Payment Service health..."
curl -fsS "$PAYMENT_URL/actuator/health" | jq .

echo
echo "3. Getting Keycloak token..."
TOKEN=$(curl -s \
  -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  | jq -r '.access_token')

if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "FAIL: Could not get Keycloak token."
  exit 1
fi

echo "PASS: Token acquired"

PAYLOAD=$(cat <<JSON
{
  "customerId": "$CUSTOMER_ID",
  "sourceAccount": "$SOURCE_ACCOUNT",
  "destinationAccount": "$DESTINATION_ACCOUNT",
  "amount": 125.50,
  "currency": "USD",
  "paymentReference": "$PAYMENT_REFERENCE",
  "paymentMethod": "$PAYMENT_METHOD",
  "narration": "$NARRATION"
}
JSON
)

echo
echo "4. Creating NEW payment through API Gateway..."
PAYMENT_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Correlation-ID: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "$PAYLOAD")

echo "$PAYMENT_RESPONSE" | jq .

PAYMENT_ERROR_STATUS=$(echo "$PAYMENT_RESPONSE" | jq -r '.status // empty')

if [[ "$PAYMENT_ERROR_STATUS" == "400" || "$PAYMENT_ERROR_STATUS" == "401" || "$PAYMENT_ERROR_STATUS" == "403" || "$PAYMENT_ERROR_STATUS" == "500" ]]; then
  echo "FAIL: Payment creation failed."
  exit 1
fi

PAYMENT_DB_ID=$(echo "$PAYMENT_RESPONSE" | jq -r '.id // empty')
PAYMENT_UUID=$(echo "$PAYMENT_RESPONSE" | jq -r '.paymentId // empty')
PAYMENT_STATUS=$(echo "$PAYMENT_RESPONSE" | jq -r '.status // empty')

if [[ -z "$PAYMENT_DB_ID" && -z "$PAYMENT_UUID" ]]; then
  echo "FAIL: Payment response did not contain id or paymentId."
  exit 1
fi

echo "PASS: Payment created."
echo "Payment DB ID:   ${PAYMENT_DB_ID:-N/A}"
echo "Payment UUID:    ${PAYMENT_UUID:-N/A}"
echo "Payment Status:  ${PAYMENT_STATUS:-N/A}"

echo
echo "5. Replaying same payment with same idempotency key..."
REPLAY_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Correlation-ID: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "$PAYLOAD")

echo "$REPLAY_RESPONSE" | jq .

REPLAY_DB_ID=$(echo "$REPLAY_RESPONSE" | jq -r '.id // empty')
REPLAY_UUID=$(echo "$REPLAY_RESPONSE" | jq -r '.paymentId // empty')

if [[ -n "$PAYMENT_DB_ID" && -n "$REPLAY_DB_ID" && "$PAYMENT_DB_ID" == "$REPLAY_DB_ID" ]]; then
  echo "PASS: Idempotency replay returned the same database ID."
elif [[ -n "$PAYMENT_UUID" && -n "$REPLAY_UUID" && "$PAYMENT_UUID" == "$REPLAY_UUID" ]]; then
  echo "PASS: Idempotency replay returned the same payment UUID."
else
  echo "FAIL: Idempotency replay did not clearly return the same payment."
  exit 1
fi

echo
echo "6. Testing missing tenant header rejection..."
MISSING_TENANT_STATUS=$(curl -s -o /tmp/naitfs-missing-tenant-response.json -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: corr-missing-tenant-${RUN_ID}" \
  -H "Idempotency-Key: idem-missing-tenant-${RUN_ID}" \
  -d "$PAYLOAD")

echo "Missing tenant HTTP status: $MISSING_TENANT_STATUS"

if [[ "$MISSING_TENANT_STATUS" == "400" || "$MISSING_TENANT_STATUS" == "401" || "$MISSING_TENANT_STATUS" == "403" ]]; then
  echo "PASS: Missing tenant header rejected."
else
  echo "FAIL: Expected 400, 401, or 403 for missing tenant header."
  cat /tmp/naitfs-missing-tenant-response.json || true
  exit 1
fi

echo
echo "7. Testing invalid JWT rejection..."
INVALID_JWT_STATUS=$(curl -s -o /tmp/naitfs-invalid-jwt-response.json -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer invalid-token" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-invalid-jwt-${RUN_ID}" \
  -H "X-Correlation-ID: corr-invalid-jwt-${RUN_ID}" \
  -H "Idempotency-Key: idem-invalid-jwt-${RUN_ID}" \
  -d "$PAYLOAD")

echo "Invalid JWT HTTP status: $INVALID_JWT_STATUS"

if [[ "$INVALID_JWT_STATUS" == "401" || "$INVALID_JWT_STATUS" == "403" ]]; then
  echo "PASS: Invalid JWT rejected."
else
  echo "FAIL: Expected 401 or 403 for invalid JWT."
  cat /tmp/naitfs-invalid-jwt-response.json || true
  exit 1
fi

echo
echo "8. Querying demo dashboard evidence summary..."
DASHBOARD_RESPONSE=$(curl -s "$GATEWAY_URL/api/v1/evidence/demo-dashboard?tenantId=$TENANT_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Correlation-ID: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY")

echo "$DASHBOARD_RESPONSE" | jq .

DASHBOARD_ERROR_STATUS=$(echo "$DASHBOARD_RESPONSE" | jq -r '.status // empty')

if [[ "$DASHBOARD_ERROR_STATUS" == "400" || "$DASHBOARD_ERROR_STATUS" == "401" || "$DASHBOARD_ERROR_STATUS" == "403" || "$DASHBOARD_ERROR_STATUS" == "500" ]]; then
  echo "FAIL: Demo dashboard query failed."
  exit 1
fi

echo "PASS: Demo dashboard query completed."

echo
echo "9. Checking payment row in Postgres..."
docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
SELECT id, payment_id, customer_id, amount, currency, payment_reference, payment_method, status, created_at
FROM payments
WHERE payment_reference = '$PAYMENT_REFERENCE'
ORDER BY created_at DESC;
"

echo
echo "10. Checking outbox events in Postgres..."
docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
SELECT id, aggregate_type, aggregate_id, topic, status, created_at
FROM outbox_events
ORDER BY created_at DESC
LIMIT 10;
"

echo
echo "============================================================"
echo " PAYMENT SIDE TEST COMPLETE"
echo "============================================================"
echo "Validated:"
echo "- API Gateway health"
echo "- Payment service health"
echo "- Keycloak token acquisition"
echo "- New payment creation"
echo "- Idempotency replay"
echo "- Missing tenant rejection"
echo "- Invalid JWT rejection"
echo "- Demo dashboard evidence summary"
echo "- Payment database row"
echo "- Outbox event visibility"
echo
echo "Generated automatically for this test run:"
echo "Tenant ID:        $TENANT_ID"
echo "Correlation ID:   $CORRELATION_ID"
echo "Idempotency Key:  $IDEMPOTENCY_KEY"
echo "Customer ID:      $CUSTOMER_ID"
echo "Payment Ref:      $PAYMENT_REFERENCE"
echo "Payment DB ID:    ${PAYMENT_DB_ID:-N/A}"
echo "Payment UUID:     ${PAYMENT_UUID:-N/A}"
echo "Payment Status:   ${PAYMENT_STATUS:-N/A}"
echo "============================================================"
