#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-payment-cli}"
USERNAME="${USERNAME:-payment-user}"
PASSWORD="${PASSWORD:-paymentpass}"
EVIDENCE_WAIT_SECONDS="${EVIDENCE_WAIT_SECONDS:-40}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-2}"

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

require_cmd curl
require_cmd jq
require_cmd uuidgen

echo "=================================================="
echo " N-AITFS MVP 9 Smoke Test"
echo "=================================================="
echo "Gateway URL: $GATEWAY_URL"
echo ""

echo "Step 1: Obtaining Keycloak token..."
TOKEN_RESPONSE="$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" \
  -d "scope=openid profile email documents.write knowledge.read payments.write payments.read evidence.read")" \
  || fail "Unable to reach Keycloak at $KEYCLOAK_URL"

TOKEN="$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')"
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "$TOKEN_RESPONSE" | jq . 2>/dev/null || echo "$TOKEN_RESPONSE"
  fail "Failed to obtain JWT token"
fi
pass "JWT token generated"
echo ""

TENANT_ID="tenant-mvp9-$(uuidgen | tr '[:upper:]' '[:lower:]' | cut -c1-8)"
CORRELATION_ID="corr-$(uuidgen | tr '[:upper:]' '[:lower:]')"
IDEMPOTENCY_KEY="idem-mvp9-$(uuidgen | tr '[:upper:]' '[:lower:]')"
CUSTOMER_ID="customer-mvp9-$(date +%s)"
AMOUNT="${AMOUNT:-2000000.99}"
CURRENCY="${CURRENCY:-USD}"
TMP_DOC="$(mktemp "${TMPDIR:-/tmp}/mvp9-regulated.XXXXXX.txt")"

cat > "$TMP_DOC" <<'DOC'
Regulated Workflow Control

The vendor onboarding control requires sanctions screening before account activation.
Payment execution must preserve tenant, correlation, and idempotency metadata.
Compliance evidence must retain document, knowledge answer, payment received, and payment completed events.
DOC

echo "Tenant ID:       $TENANT_ID"
echo "Correlation ID:  $CORRELATION_ID"
echo "Idempotency Key: $IDEMPOTENCY_KEY"
echo ""

common_headers=(
  -H "Authorization: Bearer $TOKEN"
  -H "X-Tenant-Id: $TENANT_ID"
  -H "X-Correlation-Id: $CORRELATION_ID"
  -H "Idempotency-Key: $IDEMPOTENCY_KEY"
)

echo "Step 2: Calling document process endpoint through gateway..."
TMP_PROCESS_RESPONSE="$(mktemp)"
PROCESS_STATUS="$(curl -s -o "$TMP_PROCESS_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/documents/process" \
  "${common_headers[@]}" \
  -F "file=@$TMP_DOC;type=text/plain")"

if [ "$PROCESS_STATUS" != "200" ]; then
  cat "$TMP_PROCESS_RESPONSE" | jq . 2>/dev/null || cat "$TMP_PROCESS_RESPONSE"
  fail "Document processing returned HTTP $PROCESS_STATUS"
fi

DOCUMENT_ID="$(jq -r '.objectName // empty' "$TMP_PROCESS_RESPONSE")"
rm -f "$TMP_PROCESS_RESPONSE"
if [ -z "$DOCUMENT_ID" ] || [ "$DOCUMENT_ID" = "null" ]; then
  fail "Document processing response did not include objectName"
fi
pass "Document processed with documentId=$DOCUMENT_ID"
echo ""

echo "Step 3: Calling knowledge answer endpoint through gateway using documentId..."
sleep "$POLL_INTERVAL_SECONDS"

ANSWER_BODY="$(jq -n \
  --arg query "What does the vendor onboarding control require before account activation?" \
  --arg documentId "$DOCUMENT_ID" \
  '{query: $query, documentId: $documentId}')"

TMP_ANSWER_RESPONSE="$(mktemp)"
ANSWER_STATUS="$(curl -s -o "$TMP_ANSWER_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/knowledge/answer" \
  "${common_headers[@]}" \
  -H "Content-Type: application/json" \
  -d "$ANSWER_BODY")"

if [ "$ANSWER_STATUS" != "200" ]; then
  cat "$TMP_ANSWER_RESPONSE" | jq . 2>/dev/null || cat "$TMP_ANSWER_RESPONSE"
  fail "Knowledge answer returned HTTP $ANSWER_STATUS"
fi

CITATION_COUNT="$(jq -r '.citationPackage.citations | length' "$TMP_ANSWER_RESPONSE")"
rm -f "$TMP_ANSWER_RESPONSE"
if [ "$CITATION_COUNT" -lt 1 ]; then
  fail "Knowledge answer did not include citations"
fi
pass "Knowledge answer returned $CITATION_COUNT citation(s)"
echo ""

echo "Step 4: Calling payment endpoint through gateway with same correlationId..."
PAYMENT_BODY="$(jq -n \
  --arg customerId "$CUSTOMER_ID" \
  --arg sourceAccount "ACC-SOURCE-MVP9" \
  --arg destinationAccount "ACC-DEST-MVP9" \
  --arg currency "$CURRENCY" \
  --argjson amount "$AMOUNT" \
  '{customerId: $customerId, sourceAccount: $sourceAccount, destinationAccount: $destinationAccount, amount: $amount, currency: $currency}')"

TMP_PAYMENT_RESPONSE="$(mktemp)"
PAYMENT_STATUS="$(curl -s -o "$TMP_PAYMENT_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/payments" \
  "${common_headers[@]}" \
  -H "Content-Type: application/json" \
  -d "$PAYMENT_BODY")"

if [ "$PAYMENT_STATUS" != "200" ] && [ "$PAYMENT_STATUS" != "201" ]; then
  cat "$TMP_PAYMENT_RESPONSE" | jq . 2>/dev/null || cat "$TMP_PAYMENT_RESPONSE"
  fail "Payment endpoint returned HTTP $PAYMENT_STATUS"
fi

PAYMENT_ID="$(jq -r '.paymentId // empty' "$TMP_PAYMENT_RESPONSE")"
rm -f "$TMP_PAYMENT_RESPONSE"
if [ -z "$PAYMENT_ID" ] || [ "$PAYMENT_ID" = "null" ]; then
  fail "Payment response did not include paymentId"
fi
pass "Payment workflow triggered with paymentId=$PAYMENT_ID"
echo ""

echo "Step 5: Querying compliance evidence by correlationId..."
EVIDENCE_RESPONSE=""
deadline=$((SECONDS + EVIDENCE_WAIT_SECONDS))

while [ "$SECONDS" -le "$deadline" ]; do
  EVIDENCE_RESPONSE="$(curl -s \
    "${common_headers[@]}" \
    "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID")"

  document_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "DOCUMENT_EXTRACTED")] | length')"
  knowledge_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "KNOWLEDGE_ANSWER_GENERATED")] | length')"
  received_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "PAYMENT_RECEIVED")] | length')"
  completed_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "PAYMENT_COMPLETED")] | length')"

  if [ "$document_count" -gt 0 ] \
    && [ "$knowledge_count" -gt 0 ] \
    && [ "$received_count" -gt 0 ] \
    && [ "$completed_count" -gt 0 ]; then
    break
  fi

  sleep "$POLL_INTERVAL_SECONDS"
done

document_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "DOCUMENT_EXTRACTED")] | length')"
knowledge_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "KNOWLEDGE_ANSWER_GENERATED")] | length')"
received_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "PAYMENT_RECEIVED")] | length')"
completed_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "PAYMENT_COMPLETED")] | length')"
tenant_count="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg tenantId "$TENANT_ID" '[.evidence[]? | select(.tenantId == $tenantId)] | length')"
correlation_count="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg correlationId "$CORRELATION_ID" '[.evidence[]? | select(.correlationId == $correlationId)] | length')"

if [ "$document_count" -lt 1 ] || [ "$knowledge_count" -lt 1 ] || [ "$received_count" -lt 1 ] || [ "$completed_count" -lt 1 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "Correlation evidence missing one or more regulated workflow event types"
fi

if [ "$tenant_count" -lt 4 ] || [ "$correlation_count" -lt 4 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "Correlation evidence did not preserve tenant/correlation metadata"
fi

rm -f "$TMP_DOC"

pass "Correlation evidence contains document, knowledge, payment received, and payment completed events"
echo ""
echo "=================================================="
echo " MVP 9 SMOKE TEST PASSED"
echo "=================================================="
echo "Tenant ID:      $TENANT_ID"
echo "Correlation ID: $CORRELATION_ID"
echo "Document ID:    $DOCUMENT_ID"
echo "Payment ID:     $PAYMENT_ID"
echo "=================================================="
