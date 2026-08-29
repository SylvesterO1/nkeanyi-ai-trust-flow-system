#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
PAYMENT_SERVICE_URL="${PAYMENT_SERVICE_URL:-http://localhost:8082}"
COMPLIANCE_SERVICE_URL="${COMPLIANCE_SERVICE_URL:-http://localhost:8084}"
KNOWLEDGE_SERVICE_URL="${KNOWLEDGE_SERVICE_URL:-http://localhost:8088}"
DOCUMENT_SERVICE_URL="${DOCUMENT_SERVICE_URL:-http://localhost:8089}"

HIGH_VALUE_AMOUNT="${HIGH_VALUE_AMOUNT:-2000000.99}"

echo "=================================================="
echo " N-AITFS MVP 6 Smoke Test"
echo "=================================================="
echo "Root: $ROOT_DIR"
echo ""

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: Required command not found: $cmd"
    exit 1
  fi
}

check_health() {
  local name="$1"
  local url="$2"

  local status
  status="$(curl -s "$url/actuator/health" | jq -r '.status // "DOWN"' 2>/dev/null || echo "DOWN")"

  printf "%-40s %s/actuator/health ... %s\n" "$name" "$url" "$status"

  if [ "$status" != "UP" ]; then
    echo ""
    echo "ERROR: $name is not UP."
    exit 1
  fi
}

require_cmd curl
require_cmd jq
require_cmd uuidgen

echo "Step 1: Checking service health..."
echo ""

check_health "api-gateway" "$GATEWAY_URL"
check_health "payment-orchestration-service" "$PAYMENT_SERVICE_URL"
check_health "compliance-evidence-service" "$COMPLIANCE_SERVICE_URL"
check_health "knowledge-service" "$KNOWLEDGE_SERVICE_URL"
check_health "document-intelligence-service" "$DOCUMENT_SERVICE_URL"

echo ""
echo "All service health checks are UP."
echo ""

echo "Step 2: Generating fresh JWT token..."
echo ""

./scripts/get-token.sh >/dev/null

if [ ! -f ".token" ] || [ ! -s ".token" ]; then
  echo "ERROR: Token generation failed. .token missing or empty."
  exit 1
fi

TOKEN="$(cat .token)"

echo "Token generated."
echo ""

echo "Step 3: Creating high-value payment through API Gateway..."
echo ""

AMOUNT="$HIGH_VALUE_AMOUNT" ./scripts/create-payment.sh

RESULT_FILE="target/demo-results/latest-payment.json"

if [ ! -f "$RESULT_FILE" ]; then
  echo "ERROR: Payment result file not found: $RESULT_FILE"
  exit 1
fi

PAYMENT_ID="$(jq -r '.paymentId // empty' "$RESULT_FILE")"
CORRELATION_ID="$(jq -r '.correlationId // empty' "$RESULT_FILE")"
TENANT_ID="$(jq -r '.tenantId // empty' "$RESULT_FILE")"
HTTP_STATUS="$(jq -r '.httpStatus // empty' "$RESULT_FILE")"

if [ -z "$PAYMENT_ID" ] || [ "$PAYMENT_ID" = "null" ]; then
  echo "ERROR: paymentId missing from latest payment result."
  cat "$RESULT_FILE" | jq .
  exit 1
fi

if [ -z "$CORRELATION_ID" ] || [ "$CORRELATION_ID" = "null" ]; then
  echo "ERROR: correlationId missing from latest payment result."
  cat "$RESULT_FILE" | jq .
  exit 1
fi

if [ "$HTTP_STATUS" != "201" ] && [ "$HTTP_STATUS" != "200" ]; then
  echo "ERROR: Payment creation did not return 200/201."
  cat "$RESULT_FILE" | jq .
  exit 1
fi

echo ""
echo "Payment created:"
echo "  Payment ID:     $PAYMENT_ID"
echo "  Tenant ID:      $TENANT_ID"
echo "  Correlation ID: $CORRELATION_ID"
echo "  HTTP Status:    $HTTP_STATUS"
echo ""

echo "Step 4: Waiting for async orchestration and Kafka processing..."
sleep 8
echo ""

echo "Step 5: Verifying payment reached COMPLETED in payment logs..."
echo ""

if grep "$PAYMENT_ID" logs/payment-orchestration-service.log | grep -q "outcome=COMPLETED"; then
  echo "Payment COMPLETED log found."
else
  echo "ERROR: Payment COMPLETED log not found for paymentId=$PAYMENT_ID"
  echo ""
  echo "Recent matching logs:"
  grep "$PAYMENT_ID" logs/payment-orchestration-service.log || true
  exit 1
fi

if grep "$PAYMENT_ID" logs/payment-orchestration-service.log | grep -q "Published event topic=payment.processed"; then
  echo "payment.processed publication log found."
else
  echo "ERROR: payment.processed publication log not found for paymentId=$PAYMENT_ID"
  echo ""
  echo "Recent matching logs:"
  grep "$PAYMENT_ID" logs/payment-orchestration-service.log || true
  exit 1
fi

echo ""

echo "Step 6: Verifying compliance evidence by correlation ID..."
echo ""

EVIDENCE_RESPONSE="$(curl -s "$COMPLIANCE_SERVICE_URL/api/v1/evidence/correlation/$CORRELATION_ID")"

EVIDENCE_COUNT="$(echo "$EVIDENCE_RESPONSE" | jq -r '.count // 0')"
EVIDENCE_PAYMENT_ID="$(echo "$EVIDENCE_RESPONSE" | jq -r '.evidence[0].paymentId // empty')"
EVIDENCE_DECISION="$(echo "$EVIDENCE_RESPONSE" | jq -r '.evidence[0].decision // empty')"
EVIDENCE_RISK_LEVEL="$(echo "$EVIDENCE_RESPONSE" | jq -r '.evidence[0].riskLevel // empty')"
EVIDENCE_IMMUTABLE="$(echo "$EVIDENCE_RESPONSE" | jq -r '.evidence[0].immutableRecord // false')"
EVIDENCE_PAYLOAD_HASH="$(echo "$EVIDENCE_RESPONSE" | jq -r '.evidence[0].payloadHash // empty')"
EVIDENCE_PAYLOAD="$(echo "$EVIDENCE_RESPONSE" | jq -r '.evidence[0].evidencePayload // empty')"

if [ "$EVIDENCE_COUNT" -lt 1 ]; then
  echo "ERROR: No compliance evidence found for correlationId=$CORRELATION_ID"
  echo "$EVIDENCE_RESPONSE" | jq .
  exit 1
fi

if [ "$EVIDENCE_PAYMENT_ID" != "$PAYMENT_ID" ]; then
  echo "ERROR: Evidence paymentId mismatch."
  echo "Expected: $PAYMENT_ID"
  echo "Actual:   $EVIDENCE_PAYMENT_ID"
  echo "$EVIDENCE_RESPONSE" | jq .
  exit 1
fi

if [ "$EVIDENCE_DECISION" != "COMPLETED" ]; then
  echo "ERROR: Evidence decision is not COMPLETED."
  echo "Actual: $EVIDENCE_DECISION"
  echo "$EVIDENCE_RESPONSE" | jq .
  exit 1
fi

if [ "$EVIDENCE_RISK_LEVEL" != "MEDIUM" ]; then
  echo "ERROR: Evidence riskLevel is not MEDIUM."
  echo "Actual: $EVIDENCE_RISK_LEVEL"
  echo "$EVIDENCE_RESPONSE" | jq .
  exit 1
fi

if [ "$EVIDENCE_IMMUTABLE" != "true" ]; then
  echo "ERROR: Evidence immutableRecord is not true."
  echo "$EVIDENCE_RESPONSE" | jq .
  exit 1
fi

if [ -z "$EVIDENCE_PAYLOAD_HASH" ] || [ "$EVIDENCE_PAYLOAD_HASH" = "null" ]; then
  echo "ERROR: Evidence payloadHash missing."
  echo "$EVIDENCE_RESPONSE" | jq .
  exit 1
fi

if ! echo "$EVIDENCE_PAYLOAD" | grep -q '"riskScore":55.0'; then
  echo "ERROR: Evidence payload does not include expected riskScore=55.0."
  echo "$EVIDENCE_RESPONSE" | jq .
  exit 1
fi

echo "Compliance evidence verified."
echo ""

echo "=================================================="
echo " MVP 6 SMOKE TEST PASSED"
echo "=================================================="
echo "Payment ID:      $PAYMENT_ID"
echo "Correlation ID:  $CORRELATION_ID"
echo "Decision:        $EVIDENCE_DECISION"
echo "Risk Level:      $EVIDENCE_RISK_LEVEL"
echo "Immutable:       $EVIDENCE_IMMUTABLE"
echo "Payload Hash:    $EVIDENCE_PAYLOAD_HASH"
echo "=================================================="
