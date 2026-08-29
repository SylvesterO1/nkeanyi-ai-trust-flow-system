#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
DOCUMENT_SERVICE_URL="${DOCUMENT_SERVICE_URL:-http://localhost:8089}"
KNOWLEDGE_SERVICE_URL="${KNOWLEDGE_SERVICE_URL:-http://localhost:8088}"
PAYMENT_SERVICE_URL="${PAYMENT_SERVICE_URL:-http://localhost:8082}"
COMPLIANCE_SERVICE_URL="${COMPLIANCE_SERVICE_URL:-http://localhost:8084}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-naitfs-client}"
USERNAME="${USERNAME:-test-user}"
PASSWORD="${PASSWORD:-test-password}"
EVIDENCE_WAIT_SECONDS="${EVIDENCE_WAIT_SECONDS:-60}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-2}"

TMP_FILES=()

cleanup() {
  for file in "${TMP_FILES[@]:-}"; do
    [ -n "$file" ] && [ -f "$file" ] && rm -f "$file"
  done
  return 0
}
trap cleanup EXIT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "FAIL: Required command not found: $1"
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

mktemp_track() {
  local file
  file="$(mktemp "${TMPDIR:-/tmp}/mvp19.XXXXXX")"
  TMP_FILES+=("$file")
  echo "$file"
}

json_get() {
  jq -r "$1 // empty" "$2"
}

health_check() {
  local name="$1"
  local url="$2"
  local out status
  out="$(mktemp_track)"
  status="$(curl -sS -o "$out" -w "%{http_code}" "$url/actuator/health" || true)"

  if [ "$status" != "200" ] || ! jq -e '.status == "UP"' "$out" >/dev/null 2>&1; then
    cat "$out" 2>/dev/null || true
    fail "$name is not UP at $url/actuator/health"
  fi

  pass "$name is UP"
}

gateway_json() {
  local method="$1"
  local path="$2"
  local idempotency_key="$3"
  local body="$4"
  local out="$5"

  curl -sS -o "$out" -w "%{http_code}" \
    -X "$method" "$GATEWAY_URL$path" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -H "Idempotency-Key: $idempotency_key" \
    -H "Content-Type: application/json" \
    -d "$body"
}

gateway_get() {
  local path="$1"
  local out="$2"

  curl -sS -o "$out" -w "%{http_code}" \
    "$GATEWAY_URL$path" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -H "Idempotency-Key: evidence-query-$RUN_ID"
}

query_evidence_json() {
  curl -sS \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -H "Idempotency-Key: evidence-query-$RUN_ID" \
    "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID"
}

evidence_count() {
  local evidence_json="$1"
  echo "$evidence_json" | jq -r '(.evidence // .records // .items // []) | length'
}

evidence_type_count() {
  local evidence_json="$1"
  local evidence_type="$2"
  echo "$evidence_json" | jq -r --arg type "$evidence_type" '[(.evidence // .records // .items // [])[]? | select(.evidenceType == $type)] | length'
}

evidence_any_type_count() {
  local evidence_json="$1"
  shift
  local filter='false'
  local type

  for type in "$@"; do
    filter="$filter or .evidenceType == \"$type\""
  done

  echo "$evidence_json" | jq -r "[(.evidence // .records // .items // [])[]? | select($filter)] | length"
}

wait_for_evidence_type() {
  local label="$1"
  local type="$2"
  local evidence_json=""
  local deadline=$((SECONDS + EVIDENCE_WAIT_SECONDS))

  while [ "$SECONDS" -le "$deadline" ]; do
    evidence_json="$(query_evidence_json)"
    if [ "$(evidence_type_count "$evidence_json" "$type")" -gt 0 ]; then
      echo "$evidence_json"
      return 0
    fi
    sleep "$POLL_INTERVAL_SECONDS"
  done

  echo "$evidence_json" | jq . 2>/dev/null || echo "$evidence_json"
  fail "Timed out waiting for evidence type $type ($label)"
}

wait_for_payment_evidence() {
  local evidence_json=""
  local deadline=$((SECONDS + EVIDENCE_WAIT_SECONDS))

  while [ "$SECONDS" -le "$deadline" ]; do
    evidence_json="$(query_evidence_json)"
    if [ "$(evidence_any_type_count "$evidence_json" PAYMENT_RECEIVED PAYMENT_COMPLETED PAYMENT_AUDIT PAYMENT_STATUS_CHANGED PAYMENT_DECISION_EVIDENCE)" -gt 0 ]; then
      echo "$evidence_json"
      return 0
    fi
    sleep "$POLL_INTERVAL_SECONDS"
  done

  echo "$evidence_json" | jq . 2>/dev/null || echo "$evidence_json"
  fail "Timed out waiting for payment-related evidence"
}

require_cmd curl
require_cmd jq
require_cmd uuidgen

echo "============================================================"
echo " NAITFS MVP19 Commercial Demo Readiness Smoke Test"
echo "============================================================"
echo "Gateway: $GATEWAY_URL"
echo

echo "1. Platform health"
health_check "api-gateway" "$GATEWAY_URL"
health_check "document-intelligence-service" "$DOCUMENT_SERVICE_URL"
health_check "knowledge-service" "$KNOWLEDGE_SERVICE_URL"
health_check "payment-orchestration-service" "$PAYMENT_SERVICE_URL"
health_check "compliance-evidence-service" "$COMPLIANCE_SERVICE_URL"

KEYCLOAK_STATUS="$(curl -sS -o /dev/null -w "%{http_code}" "$KEYCLOAK_URL/realms/$REALM" || true)"
[ "$KEYCLOAK_STATUS" = "200" ] || fail "Keycloak realm $REALM not reachable at $KEYCLOAK_URL"
pass "Keycloak realm is reachable"
echo

echo "2. Keycloak token acquisition"
TOKEN_RESPONSE="$(curl -sS -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" \
  -d "scope=openid profile email documents.write knowledge.read payments.write payments.read evidence.read")" \
  || fail "Unable to reach Keycloak token endpoint"

TOKEN="$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')"
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || { echo "$TOKEN_RESPONSE" | jq . 2>/dev/null || echo "$TOKEN_RESPONSE"; fail "Failed to obtain Keycloak token"; }
pass "Keycloak token acquired for $USERNAME"
echo

RUN_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
TENANT_ID="tenant-mvp19-${RUN_ID:0:8}"
CORRELATION_ID="corr-mvp19-$RUN_ID"
DOC_IDEM="idem-mvp19-doc-$RUN_ID"
KNOWLEDGE_IDEM="idem-mvp19-knowledge-$RUN_ID"
PAYMENT_IDEM="idem-mvp19-payment-$RUN_ID"
CUSTOMER_ID="customer-mvp19-${RUN_ID:0:8}"
PAYMENT_REFERENCE="MVP19-$RUN_ID"

echo "Commercial demo context"
echo "Tenant ID:       $TENANT_ID"
echo "Correlation ID:  $CORRELATION_ID"
echo "Idempotency Key: $PAYMENT_IDEM"
echo "Customer ID:     $CUSTOMER_ID"
echo "Payment Ref:     $PAYMENT_REFERENCE"
echo

echo "3. Secured gateway access"
EVIDENCE_EMPTY="$(mktemp_track)"
EVIDENCE_EMPTY_STATUS="$(gateway_get "/api/v1/evidence/correlation/$CORRELATION_ID" "$EVIDENCE_EMPTY")"
[ "$EVIDENCE_EMPTY_STATUS" = "200" ] || { cat "$EVIDENCE_EMPTY"; fail "Secured evidence query returned HTTP $EVIDENCE_EMPTY_STATUS"; }
pass "Secured gateway request accepted with JWT, tenant, correlation, and idempotency headers"
echo

echo "4. Document intelligence"
DOC_FILE="$(mktemp_track)"
cat > "$DOC_FILE" <<'DOC'
NAITFS MVP19 Commercial Demo Control

This regulated workflow requires identity enforcement, tenant isolation, correlation tracing,
idempotent payment execution, grounded knowledge review, and compliance evidence capture before
production approval.
DOC

DOC_RESPONSE="$(mktemp_track)"
DOC_STATUS="$(curl -sS -o "$DOC_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/documents/process" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $DOC_IDEM" \
  -F "file=@$DOC_FILE;filename=mvp19-commercial-demo.txt;type=text/plain")"
[ "$DOC_STATUS" = "200" ] || { cat "$DOC_RESPONSE" | jq . 2>/dev/null || cat "$DOC_RESPONSE"; fail "Document processing returned HTTP $DOC_STATUS"; }
DOCUMENT_ID="$(json_get '.documentId // .objectName' "$DOC_RESPONSE")"
[ -n "$DOCUMENT_ID" ] || { cat "$DOC_RESPONSE" | jq .; fail "Document response did not include documentId/objectName"; }
pass "Document processed: $DOCUMENT_ID"

EVIDENCE_JSON="$(wait_for_evidence_type "document processing" DOCUMENT_EXTRACTED)"
pass "Document evidence recorded"
echo

echo "5. Grounded knowledge answer"
ANSWER_BODY="$(jq -n \
  --arg query "What controls must this workflow prove before production approval?" \
  --arg documentId "$DOCUMENT_ID" \
  '{query: $query, documentId: $documentId}')"
ANSWER_RESPONSE="$(mktemp_track)"
ANSWER_STATUS="$(gateway_json POST "/api/v1/knowledge/answer" "$KNOWLEDGE_IDEM" "$ANSWER_BODY" "$ANSWER_RESPONSE")"
[ "$ANSWER_STATUS" = "200" ] || { cat "$ANSWER_RESPONSE" | jq . 2>/dev/null || cat "$ANSWER_RESPONSE"; fail "Knowledge answer returned HTTP $ANSWER_STATUS"; }
ANSWER_TEXT="$(json_get '.answer' "$ANSWER_RESPONSE")"
[ -n "$ANSWER_TEXT" ] || fail "Knowledge answer response did not include answer text"
pass "Grounded knowledge answer generated"

EVIDENCE_JSON="$(wait_for_evidence_type "knowledge answer" KNOWLEDGE_ANSWER_GENERATED)"
pass "Knowledge evidence recorded"
echo

echo "6. Payment orchestration"
PAYMENT_BODY="$(jq -n \
  --arg customerId "$CUSTOMER_ID" \
  --arg sourceAccount "ACC-MVP19-SOURCE" \
  --arg destinationAccount "ACC-MVP19-DEST" \
  --arg paymentReference "$PAYMENT_REFERENCE" \
  '{customerId: $customerId, sourceAccount: $sourceAccount, destinationAccount: $destinationAccount, amount: 1250.75, currency: "USD", paymentReference: $paymentReference, paymentMethod: "BANK_TRANSFER", narration: "MVP19 commercial trust-flow demo"}')"

PAYMENT_RESPONSE="$(mktemp_track)"
PAYMENT_STATUS="$(gateway_json POST "/api/v1/payments" "$PAYMENT_IDEM" "$PAYMENT_BODY" "$PAYMENT_RESPONSE")"
if [ "$PAYMENT_STATUS" != "201" ] && [ "$PAYMENT_STATUS" != "200" ]; then
  cat "$PAYMENT_RESPONSE" | jq . 2>/dev/null || cat "$PAYMENT_RESPONSE"
  fail "Payment creation returned HTTP $PAYMENT_STATUS"
fi
PAYMENT_ID="$(json_get '.paymentId' "$PAYMENT_RESPONSE")"
[ -n "$PAYMENT_ID" ] || { cat "$PAYMENT_RESPONSE" | jq .; fail "Payment response did not include paymentId"; }
pass "Payment created: $PAYMENT_ID"

PAYMENT_REPLAY_RESPONSE="$(mktemp_track)"
PAYMENT_REPLAY_STATUS="$(gateway_json POST "/api/v1/payments" "$PAYMENT_IDEM" "$PAYMENT_BODY" "$PAYMENT_REPLAY_RESPONSE")"
[ "$PAYMENT_REPLAY_STATUS" = "200" ] || { cat "$PAYMENT_REPLAY_RESPONSE" | jq . 2>/dev/null || cat "$PAYMENT_REPLAY_RESPONSE"; fail "Expected idempotency replay HTTP 200, got $PAYMENT_REPLAY_STATUS"; }
REPLAY_PAYMENT_ID="$(json_get '.paymentId' "$PAYMENT_REPLAY_RESPONSE")"
[ "$REPLAY_PAYMENT_ID" = "$PAYMENT_ID" ] || fail "Idempotency replay returned a different paymentId"
pass "Idempotency replay returned original payment without unsafe duplicate"

EVIDENCE_JSON="$(wait_for_payment_evidence)"
pass "Payment-related evidence recorded"
echo

echo "7. Evidence ledger verification"
EVIDENCE_COUNT="$(evidence_count "$EVIDENCE_JSON")"
PAYMENT_EVIDENCE_COUNT="$(evidence_any_type_count "$EVIDENCE_JSON" PAYMENT_RECEIVED PAYMENT_COMPLETED PAYMENT_AUDIT PAYMENT_STATUS_CHANGED PAYMENT_DECISION_EVIDENCE)"
DOCUMENT_EVIDENCE_COUNT="$(evidence_type_count "$EVIDENCE_JSON" DOCUMENT_EXTRACTED)"
KNOWLEDGE_EVIDENCE_COUNT="$(evidence_type_count "$EVIDENCE_JSON" KNOWLEDGE_ANSWER_GENERATED)"
TENANT_MISMATCH_COUNT="$(echo "$EVIDENCE_JSON" | jq -r --arg tenant "$TENANT_ID" '[(.evidence // .records // .items // [])[]? | select(.tenantId != $tenant)] | length')"
CORRELATION_MISMATCH_COUNT="$(echo "$EVIDENCE_JSON" | jq -r --arg corr "$CORRELATION_ID" '[(.evidence // .records // .items // [])[]? | select(.correlationId != $corr)] | length')"

[ "$EVIDENCE_COUNT" -gt 0 ] || fail "No evidence records found"
[ "$PAYMENT_EVIDENCE_COUNT" -gt 0 ] || fail "No payment-related evidence found"
[ "$DOCUMENT_EVIDENCE_COUNT" -gt 0 ] || fail "No document evidence found"
[ "$KNOWLEDGE_EVIDENCE_COUNT" -gt 0 ] || fail "No knowledge evidence found"
[ "$TENANT_MISMATCH_COUNT" = "0" ] || fail "Evidence tenant propagation mismatch detected"
[ "$CORRELATION_MISMATCH_COUNT" = "0" ] || fail "Evidence correlation propagation mismatch detected"
pass "Evidence ledger contains document, knowledge, and payment evidence with tenant/correlation propagation"
echo

FINAL_DECISION="$(echo "$EVIDENCE_JSON" | jq -r '(.evidence // .records // .items // []) | map(.decision // empty) | map(select(. != "")) | .[0] // "RECORDED"')"
RISK_LEVEL="$(echo "$EVIDENCE_JSON" | jq -r '(.evidence // .records // .items // []) | map(.riskLevel // empty) | map(select(. != "")) | .[0] // "LOW"')"

cat <<SUMMARY
============================================================
MVP19 COMMERCIAL DEMO SUMMARY
============================================================
Workflow status:       COMPLETE
Tenant ID:             $TENANT_ID
Correlation ID:        $CORRELATION_ID
Idempotency Key:       $PAYMENT_IDEM
Document ID:           $DOCUMENT_ID
Payment ID:            $PAYMENT_ID
Evidence count:        $EVIDENCE_COUNT
Final decision:        $FINAL_DECISION
Risk level:            $RISK_LEVEL
Services involved:     api-gateway, document-intelligence-service, knowledge-service, payment-orchestration-service, compliance-evidence-service

READY FOR COMMERCIAL DEMO:
NAITFS verified authenticated TrustFlow execution, tenant propagation, correlation tracing, document intelligence, grounded knowledge, idempotent payment orchestration, and compliance evidence capture.
============================================================
SUMMARY
