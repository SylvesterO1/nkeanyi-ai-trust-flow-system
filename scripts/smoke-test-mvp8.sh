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
EVIDENCE_WAIT_SECONDS="${EVIDENCE_WAIT_SECONDS:-30}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-2}"

echo "=================================================="
echo " N-AITFS MVP 8 Smoke Test"
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

require_cmd curl
require_cmd jq
require_cmd uuidgen

echo "Step 1: Generating JWT token..."
echo ""

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
  fail "Failed to generate JWT token"
fi

pass "JWT token generated"
echo ""

echo "Step 2: Creating tenant, correlation, and sample document..."
echo ""

TENANT_ID="tenant-mvp8-$(uuidgen | tr '[:upper:]' '[:lower:]' | cut -c1-8)"
CORRELATION_ID="corr-$(uuidgen | tr '[:upper:]' '[:lower:]')"
IDEMPOTENCY_KEY="idem-mvp8-$(uuidgen | tr '[:upper:]' '[:lower:]')"
CUSTOMER_ID="customer-mvp8-$(date +%s)"
AMOUNT="${AMOUNT:-2000000.99}"
CURRENCY="${CURRENCY:-USD}"
TMP_DOC="$(mktemp "${TMPDIR:-/tmp}/mvp8-contract.XXXXXX.txt")"

cat > "$TMP_DOC" <<'DOC'
Contract Control Summary

The vendor onboarding policy requires sanctions screening before account activation.
The evidence retention period for onboarding decisions is seven years.
MVP8 validation should cite this document when answering onboarding policy questions.
DOC

echo "Tenant ID:      $TENANT_ID"
echo "Correlation ID: $CORRELATION_ID"
echo "Idempotency:    $IDEMPOTENCY_KEY"
echo "Document file:  $TMP_DOC"
echo ""

echo "Step 3: Processing document through Document Intelligence..."
echo ""

TMP_PROCESS_RESPONSE="$(mktemp)"
PROCESS_STATUS="$(curl -s -o "$TMP_PROCESS_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/documents/process" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -F "file=@$TMP_DOC;type=text/plain")"

if [ "$PROCESS_STATUS" != "200" ]; then
  cat "$TMP_PROCESS_RESPONSE" | jq . 2>/dev/null || cat "$TMP_PROCESS_RESPONSE"
  fail "Document processing returned HTTP $PROCESS_STATUS"
fi

DOCUMENT_ID="$(jq -r '.objectName // empty' "$TMP_PROCESS_RESPONSE")"
rm -f "$TMP_PROCESS_RESPONSE"

if [ -z "$DOCUMENT_ID" ] || [ "$DOCUMENT_ID" = "null" ]; then
  fail "Document processing response did not include objectName/documentId"
fi

pass "Document processed with documentId=$DOCUMENT_ID"
echo ""

echo "Step 4: Waiting for document.extracted.v1 to be consumed by Knowledge Service..."
sleep "$POLL_INTERVAL_SECONDS"
echo ""

echo "Step 5: Asking Knowledge Service with documentId..."
echo ""

ANSWER_BODY="$(jq -n \
  --arg query "What does the onboarding policy require before account activation?" \
  --arg documentId "$DOCUMENT_ID" \
  '{query: $query, documentId: $documentId}')"

TMP_ANSWER_RESPONSE="$(mktemp)"
ANSWER_STATUS="$(curl -s -o "$TMP_ANSWER_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/knowledge/answer" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -d "$ANSWER_BODY")"

if [ "$ANSWER_STATUS" != "200" ]; then
  cat "$TMP_ANSWER_RESPONSE" | jq . 2>/dev/null || cat "$TMP_ANSWER_RESPONSE"
  fail "Knowledge answer returned HTTP $ANSWER_STATUS"
fi

ANSWER_TEXT="$(jq -r '.answer // empty' "$TMP_ANSWER_RESPONSE")"
CITATION_COUNT="$(jq -r '.citationPackage.citations | length' "$TMP_ANSWER_RESPONSE")"

if [ -z "$ANSWER_TEXT" ] || [ "$ANSWER_TEXT" = "null" ]; then
  cat "$TMP_ANSWER_RESPONSE" | jq .
  fail "Knowledge answer did not include answer text"
fi

if [ "$CITATION_COUNT" -lt 1 ]; then
  cat "$TMP_ANSWER_RESPONSE" | jq .
  fail "Knowledge answer did not include citations"
fi

rm -f "$TMP_ANSWER_RESPONSE"
pass "Knowledge answer returned citation package with $CITATION_COUNT citation(s)"
echo ""

echo "Step 6: Triggering payment workflow with same tenant/correlation..."
echo ""

PAYMENT_BODY="$(jq -n \
  --arg customerId "$CUSTOMER_ID" \
  --arg sourceAccount "ACC-SOURCE-MVP8" \
  --arg destinationAccount "ACC-DEST-MVP8" \
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
PAYMENT_STATUS="$(curl -s -o "$TMP_PAYMENT_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "$PAYMENT_BODY")"

if [ "$PAYMENT_STATUS" != "200" ] && [ "$PAYMENT_STATUS" != "201" ]; then
  cat "$TMP_PAYMENT_RESPONSE" | jq . 2>/dev/null || cat "$TMP_PAYMENT_RESPONSE"
  fail "Payment workflow returned HTTP $PAYMENT_STATUS"
fi

PAYMENT_ID="$(jq -r '.paymentId // empty' "$TMP_PAYMENT_RESPONSE")"
rm -f "$TMP_PAYMENT_RESPONSE"

if [ -z "$PAYMENT_ID" ] || [ "$PAYMENT_ID" = "null" ]; then
  fail "Payment response did not include paymentId"
fi

pass "Payment workflow triggered with paymentId=$PAYMENT_ID"
echo ""

echo "Step 7: Confirming compliance evidence for full correlationId..."
echo ""

EVIDENCE_RESPONSE=""
deadline=$((SECONDS + EVIDENCE_WAIT_SECONDS))

while [ "$SECONDS" -le "$deadline" ]; do
  EVIDENCE_RESPONSE="$(curl -s \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID")"

  knowledge_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "KNOWLEDGE_ANSWER_GENERATED")] | length')"
  document_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "DOCUMENT_EXTRACTED")] | length')"
  payment_count="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg paymentId "$PAYMENT_ID" '[.evidence[]? | select((.paymentId // "") == $paymentId)] | length')"

  if [ "$knowledge_count" -gt 0 ] && [ "$document_count" -gt 0 ] && [ "$payment_count" -gt 0 ]; then
    break
  fi

  sleep "$POLL_INTERVAL_SECONDS"
done

knowledge_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "KNOWLEDGE_ANSWER_GENERATED")] | length')"
document_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.evidenceType == "DOCUMENT_EXTRACTED")] | length')"
payment_count="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg paymentId "$PAYMENT_ID" '[.evidence[]? | select((.paymentId // "") == $paymentId)] | length')"
tenant_match_count="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg tenantId "$TENANT_ID" '[.evidence[]? | select(.tenantId == $tenantId)] | length')"
correlation_match_count="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg correlationId "$CORRELATION_ID" '[.evidence[]? | select(.correlationId == $correlationId)] | length')"
immutable_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select(.immutableRecord == true)] | length')"
hash_count="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]? | select((.payloadHash // "") != "")] | length')"

if [ "$knowledge_count" -lt 1 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "No KNOWLEDGE_ANSWER_GENERATED evidence found for documentId=$DOCUMENT_ID"
fi

if [ "$document_count" -lt 1 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "No DOCUMENT_EXTRACTED evidence found for correlationId=$CORRELATION_ID"
fi

if [ "$payment_count" -lt 1 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "No payment/business workflow evidence found for paymentId=$PAYMENT_ID under correlationId=$CORRELATION_ID"
fi

if [ "$tenant_match_count" -lt 3 ] || [ "$correlation_match_count" -lt 3 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "Full correlation evidence did not consistently include tenantId/correlationId"
fi

if [ "$immutable_count" -lt 3 ] || [ "$hash_count" -lt 3 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "Evidence records were not immutable with payload hashes"
fi

pass "Compliance evidence recorded document, knowledge answer, and payment workflow for correlationId=$CORRELATION_ID"
echo ""

rm -f "$TMP_DOC"

echo "=================================================="
echo " MVP 8 SMOKE TEST PASSED"
echo "=================================================="
echo "Tenant ID:      $TENANT_ID"
echo "Correlation ID: $CORRELATION_ID"
echo "Document ID:    $DOCUMENT_ID"
echo "Payment ID:     $PAYMENT_ID"
echo "Citations:      $CITATION_COUNT"
echo "=================================================="
