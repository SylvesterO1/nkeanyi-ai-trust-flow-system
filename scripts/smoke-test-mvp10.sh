#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
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
EVIDENCE_WAIT_SECONDS="${EVIDENCE_WAIT_SECONDS:-45}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-2}"
AMOUNT="${AMOUNT:-2000000.99}"
CURRENCY="${CURRENCY:-USD}"

TMP_FILES=()

cleanup() {
  for file in "${TMP_FILES[@]:-}"; do
    [ -n "$file" ] && [ -f "$file" ] && rm -f "$file"
  done
  return 0
}
trap cleanup EXIT

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

mktemp_track() {
  local file
  file="$(mktemp "${TMPDIR:-/tmp}/mvp10.XXXXXX")"
  TMP_FILES+=("$file")
  echo "$file"
}

json_get() {
  jq -r "$1 // empty" "$2"
}

http_health() {
  local name="$1"
  local url="$2"
  local tmp status
  tmp="$(mktemp_track)"
  status="$(curl -sS -o "$tmp" -w "%{http_code}" "$url/actuator/health" || true)"
  if [ "$status" != "200" ]; then
    cat "$tmp" 2>/dev/null || true
    fail "$name is not reachable at $url/actuator/health (HTTP $status)"
  fi
  pass "$name reachable"
}

call_json_status() {
  local method="$1"
  local url="$2"
  local token="$3"
  local tenant_id="$4"
  local correlation_id="$5"
  local idempotency_key="$6"
  local body="$7"
  local out="$8"

  curl -sS -o "$out" -w "%{http_code}" \
    -X "$method" "$url" \
    -H "Authorization: Bearer $token" \
    -H "X-Tenant-Id: $tenant_id" \
    -H "X-Correlation-Id: $correlation_id" \
    -H "Idempotency-Key: $idempotency_key" \
    -H "Content-Type: application/json" \
    -d "$body"
}

query_evidence() {
  curl -sS \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -H "Idempotency-Key: evidence-query-$RUN_ID" \
    "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID"
}

evidence_count() {
  local evidence_json="$1"
  local evidence_type="$2"
  echo "$evidence_json" | jq -r --arg type "$evidence_type" '[.evidence[]? | select(.evidenceType == $type)] | length'
}

wait_for_evidence_types() {
  local label="$1"
  shift
  local evidence_json
  local deadline=$((SECONDS + EVIDENCE_WAIT_SECONDS))

  while [ "$SECONDS" -le "$deadline" ]; do
    evidence_json="$(query_evidence)"
    local found_all=1
    for type in "$@"; do
      if [ "$(evidence_count "$evidence_json" "$type")" -lt 1 ]; then
        found_all=0
        break
      fi
    done

    if [ "$found_all" -eq 1 ]; then
      echo "$evidence_json"
      return 0
    fi

    sleep "$POLL_INTERVAL_SECONDS"
  done

  echo "$evidence_json" | jq . 2>/dev/null || echo "$evidence_json"
  fail "Timed out waiting for evidence: $label"
}

require_cmd curl
require_cmd jq
require_cmd uuidgen

echo "=================================================="
echo " N-AITFS MVP 10 Smoke Test"
echo "=================================================="
echo "Gateway URL: $GATEWAY_URL"
echo ""

echo "Step 1: Checking service health..."
http_health "api-gateway" "$GATEWAY_URL"
http_health "document-intelligence-service" "$DOCUMENT_SERVICE_URL"
http_health "knowledge-service" "$KNOWLEDGE_SERVICE_URL"
http_health "payment-orchestration-service" "$PAYMENT_SERVICE_URL"
http_health "compliance-evidence-service" "$COMPLIANCE_SERVICE_URL"
pass "Gateway and downstream services are reachable"
echo ""

echo "Step 2: Obtaining Keycloak token..."
TOKEN_RESPONSE="$(curl -sS -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
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
pass "JWT token obtained from Keycloak"
echo ""

RUN_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
TENANT_ID="tenant-mvp10-${RUN_ID:0:8}"
CORRELATION_ID="corr-$RUN_ID"
DOCUMENT_ID=""
PAYMENT_ID=""

DOC_IDEM="idem-doc-$RUN_ID"
KNOWLEDGE_IDEM="idem-knowledge-$RUN_ID"
PAYMENT_IDEM="idem-payment-$RUN_ID"
DOC_FAIL_IDEM="idem-doc-fail-$RUN_ID"
KNOWLEDGE_FAIL_IDEM="idem-knowledge-fail-$RUN_ID"

SUCCESS_DOC="$(mktemp_track)"
EMPTY_DOC="$(mktemp_track)"

cat > "$SUCCESS_DOC" <<'DOC'
MVP10 Regulated Workflow Control

The vendor onboarding control requires sanctions screening before account activation.
Payment execution must preserve tenant, correlation, and idempotency metadata.
Compliance evidence must retain document, knowledge answer, payment received, and payment completed events.
DOC
: > "$EMPTY_DOC"

echo "Tenant ID:      $TENANT_ID"
echo "Correlation ID: $CORRELATION_ID"
echo ""

echo "Step 3: Verifying gateway rejects missing tenant/correlation headers..."
MISSING_HEADER_RESPONSE="$(mktemp_track)"
MISSING_HEADER_STATUS="$(curl -sS -o "$MISSING_HEADER_RESPONSE" -w "%{http_code}" \
  -X GET "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: missing-header-$RUN_ID" || true)"
if [ "$MISSING_HEADER_STATUS" != "400" ]; then
  cat "$MISSING_HEADER_RESPONSE" | jq . 2>/dev/null || cat "$MISSING_HEADER_RESPONSE"
  fail "Expected missing tenant/correlation request to return HTTP 400, got $MISSING_HEADER_STATUS"
fi
pass "Gateway rejects missing tenant/correlation headers"

echo "Step 4: Verifying gateway rejects invalid JWT..."
INVALID_JWT_RESPONSE="$(mktemp_track)"
INVALID_JWT_STATUS="$(curl -sS -o "$INVALID_JWT_RESPONSE" -w "%{http_code}" \
  -X GET "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID" \
  -H "Authorization: Bearer invalid.jwt.token" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: invalid-jwt-$RUN_ID" || true)"
if [ "$INVALID_JWT_STATUS" != "401" ]; then
  cat "$INVALID_JWT_RESPONSE" | jq . 2>/dev/null || cat "$INVALID_JWT_RESPONSE"
  fail "Expected invalid JWT request to return HTTP 401, got $INVALID_JWT_STATUS"
fi
pass "Gateway rejects invalid JWT"
echo ""

echo "Step 5: Processing document through gateway..."
DOC_RESPONSE="$(mktemp_track)"
DOC_STATUS="$(curl -sS -o "$DOC_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/documents/process" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $DOC_IDEM" \
  -F "file=@$SUCCESS_DOC;type=text/plain")"
if [ "$DOC_STATUS" != "200" ]; then
  cat "$DOC_RESPONSE" | jq . 2>/dev/null || cat "$DOC_RESPONSE"
  fail "Document processing returned HTTP $DOC_STATUS"
fi
DOCUMENT_ID="$(json_get '.objectName' "$DOC_RESPONSE")"
if [ -z "$DOCUMENT_ID" ]; then
  cat "$DOC_RESPONSE" | jq .
  fail "Document processing response did not include objectName"
fi
pass "Document processed through gateway with documentId=$DOCUMENT_ID"

EVIDENCE_RESPONSE="$(wait_for_evidence_types "document extraction" DOCUMENT_EXTRACTED)"
DOC_EXTRACTED_BEFORE_DUP="$(evidence_count "$EVIDENCE_RESPONSE" DOCUMENT_EXTRACTED)"

echo "Step 6: Replaying duplicate document idempotency request..."
DUP_DOC_RESPONSE="$(mktemp_track)"
DUP_DOC_STATUS="$(curl -sS -o "$DUP_DOC_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/documents/process" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $DOC_IDEM" \
  -F "file=@$SUCCESS_DOC;type=text/plain")"
if [ "$DUP_DOC_STATUS" != "200" ] && [ "$DUP_DOC_STATUS" != "409" ]; then
  cat "$DUP_DOC_RESPONSE" | jq . 2>/dev/null || cat "$DUP_DOC_RESPONSE"
  fail "Duplicate document request returned unexpected HTTP $DUP_DOC_STATUS"
fi
sleep "$POLL_INTERVAL_SECONDS"
EVIDENCE_RESPONSE="$(query_evidence)"
DOC_EXTRACTED_AFTER_DUP="$(evidence_count "$EVIDENCE_RESPONSE" DOCUMENT_EXTRACTED)"
if [ "$DOC_EXTRACTED_AFTER_DUP" -ne "$DOC_EXTRACTED_BEFORE_DUP" ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "Duplicate document idempotency request created duplicate DOCUMENT_EXTRACTED evidence"
fi
pass "Duplicate document request safely replayed/rejected without duplicate evidence"
echo ""

echo "Step 7: Asking knowledge question through gateway..."
ANSWER_BODY="$(jq -n \
  --arg query "What does the vendor onboarding control require before account activation?" \
  --arg documentId "$DOCUMENT_ID" \
  '{query: $query, documentId: $documentId}')"
ANSWER_RESPONSE="$(mktemp_track)"
ANSWER_STATUS="$(call_json_status POST "$GATEWAY_URL/api/v1/knowledge/answer" "$TOKEN" "$TENANT_ID" "$CORRELATION_ID" "$KNOWLEDGE_IDEM" "$ANSWER_BODY" "$ANSWER_RESPONSE")"
if [ "$ANSWER_STATUS" != "200" ]; then
  cat "$ANSWER_RESPONSE" | jq . 2>/dev/null || cat "$ANSWER_RESPONSE"
  fail "Knowledge answer returned HTTP $ANSWER_STATUS"
fi
CITATION_COUNT="$(jq -r '.citationPackage.citations | length' "$ANSWER_RESPONSE")"
if [ "$CITATION_COUNT" -lt 1 ]; then
  cat "$ANSWER_RESPONSE" | jq .
  fail "Knowledge answer did not include citations"
fi
pass "Knowledge answer returned citation package with $CITATION_COUNT citation(s)"

EVIDENCE_RESPONSE="$(wait_for_evidence_types "knowledge answer" DOCUMENT_EXTRACTED KNOWLEDGE_ANSWER_GENERATED)"
KNOWLEDGE_GENERATED_BEFORE_DUP="$(evidence_count "$EVIDENCE_RESPONSE" KNOWLEDGE_ANSWER_GENERATED)"

echo "Step 8: Replaying duplicate knowledge answer idempotency request..."
DUP_ANSWER_RESPONSE="$(mktemp_track)"
DUP_ANSWER_STATUS="$(call_json_status POST "$GATEWAY_URL/api/v1/knowledge/answer" "$TOKEN" "$TENANT_ID" "$CORRELATION_ID" "$KNOWLEDGE_IDEM" "$ANSWER_BODY" "$DUP_ANSWER_RESPONSE")"
if [ "$DUP_ANSWER_STATUS" != "200" ] && [ "$DUP_ANSWER_STATUS" != "409" ]; then
  cat "$DUP_ANSWER_RESPONSE" | jq . 2>/dev/null || cat "$DUP_ANSWER_RESPONSE"
  fail "Duplicate knowledge request returned unexpected HTTP $DUP_ANSWER_STATUS"
fi
sleep "$POLL_INTERVAL_SECONDS"
EVIDENCE_RESPONSE="$(query_evidence)"
KNOWLEDGE_GENERATED_AFTER_DUP="$(evidence_count "$EVIDENCE_RESPONSE" KNOWLEDGE_ANSWER_GENERATED)"
if [ "$KNOWLEDGE_GENERATED_AFTER_DUP" -ne "$KNOWLEDGE_GENERATED_BEFORE_DUP" ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "Duplicate knowledge idempotency request created duplicate KNOWLEDGE_ANSWER_GENERATED evidence"
fi
pass "Duplicate knowledge request safely replayed/rejected without duplicate evidence"
echo ""

echo "Step 9: Triggering payment workflow through gateway..."
PAYMENT_BODY="$(jq -n \
  --arg customerId "customer-mvp10-$(date +%s)" \
  --arg sourceAccount "ACC-SOURCE-MVP10" \
  --arg destinationAccount "ACC-DEST-MVP10" \
  --arg currency "$CURRENCY" \
  --argjson amount "$AMOUNT" \
  '{customerId: $customerId, sourceAccount: $sourceAccount, destinationAccount: $destinationAccount, amount: $amount, currency: $currency}')"
PAYMENT_RESPONSE="$(mktemp_track)"
PAYMENT_STATUS="$(call_json_status POST "$GATEWAY_URL/api/v1/payments" "$TOKEN" "$TENANT_ID" "$CORRELATION_ID" "$PAYMENT_IDEM" "$PAYMENT_BODY" "$PAYMENT_RESPONSE")"
if [ "$PAYMENT_STATUS" != "200" ] && [ "$PAYMENT_STATUS" != "201" ]; then
  cat "$PAYMENT_RESPONSE" | jq . 2>/dev/null || cat "$PAYMENT_RESPONSE"
  fail "Payment request returned HTTP $PAYMENT_STATUS"
fi
PAYMENT_ID="$(json_get '.paymentId' "$PAYMENT_RESPONSE")"
if [ -z "$PAYMENT_ID" ]; then
  cat "$PAYMENT_RESPONSE" | jq .
  fail "Payment response did not include paymentId"
fi
pass "Payment request accepted with paymentId=$PAYMENT_ID"
echo ""

echo "Step 10: Producing document failure evidence..."
DOC_FAIL_RESPONSE="$(mktemp_track)"
DOC_FAIL_STATUS="$(curl -sS -o "$DOC_FAIL_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/documents/process" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $DOC_FAIL_IDEM" \
  -F "file=@$EMPTY_DOC;filename=empty.txt;type=text/plain" || true)"
if [ "$DOC_FAIL_STATUS" -lt 400 ]; then
  cat "$DOC_FAIL_RESPONSE" | jq . 2>/dev/null || cat "$DOC_FAIL_RESPONSE"
  fail "Expected empty document processing to fail, got HTTP $DOC_FAIL_STATUS"
fi
pass "Document failure path returned HTTP $DOC_FAIL_STATUS"

echo "Step 11: Producing knowledge failure evidence..."
KNOWLEDGE_FAIL_BODY="$(jq -n \
  --arg query "What does this missing document say about activation?" \
  --arg documentId "missing-document-$RUN_ID" \
  '{query: $query, documentId: $documentId}')"
KNOWLEDGE_FAIL_RESPONSE="$(mktemp_track)"
KNOWLEDGE_FAIL_STATUS="$(call_json_status POST "$GATEWAY_URL/api/v1/knowledge/answer" "$TOKEN" "$TENANT_ID" "$CORRELATION_ID" "$KNOWLEDGE_FAIL_IDEM" "$KNOWLEDGE_FAIL_BODY" "$KNOWLEDGE_FAIL_RESPONSE")"
if [ "$KNOWLEDGE_FAIL_STATUS" != "200" ]; then
  cat "$KNOWLEDGE_FAIL_RESPONSE" | jq . 2>/dev/null || cat "$KNOWLEDGE_FAIL_RESPONSE"
  fail "Knowledge failure request returned HTTP $KNOWLEDGE_FAIL_STATUS"
fi
if ! jq -e '.answer | test("could not find grounded evidence"; "i")' "$KNOWLEDGE_FAIL_RESPONSE" >/dev/null; then
  cat "$KNOWLEDGE_FAIL_RESPONSE" | jq .
  fail "Knowledge failure request did not return grounded-evidence fallback"
fi
pass "Knowledge failure path returned grounded fallback"
echo ""

echo "Step 12: Querying compliance evidence package by correlationId..."
EVIDENCE_RESPONSE="$(wait_for_evidence_types \
  "complete MVP10 evidence package" \
  DOCUMENT_EXTRACTED \
  KNOWLEDGE_ANSWER_GENERATED \
  PAYMENT_RECEIVED \
  PAYMENT_COMPLETED \
  DOCUMENT_EXTRACTION_FAILED \
  KNOWLEDGE_ANSWER_FAILED)"

EVIDENCE_COUNT="$(echo "$EVIDENCE_RESPONSE" | jq -r '.count // (.evidence | length)')"
SUCCESS_EVIDENCE_TYPES="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]?.evidenceType | select(test("_FAILED$") | not)] | unique | join(", ")')"
FAILURE_EVIDENCE_TYPES="$(echo "$EVIDENCE_RESPONSE" | jq -r '[.evidence[]?.evidenceType | select(test("_FAILED$"))] | unique | join(", ")')"

TENANT_MATCH_COUNT="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg tenantId "$TENANT_ID" '[.evidence[]? | select(.tenantId == $tenantId)] | length')"
CORRELATION_MATCH_COUNT="$(echo "$EVIDENCE_RESPONSE" | jq -r --arg correlationId "$CORRELATION_ID" '[.evidence[]? | select(.correlationId == $correlationId)] | length')"
if [ "$TENANT_MATCH_COUNT" -lt 6 ] || [ "$CORRELATION_MATCH_COUNT" -lt 6 ]; then
  echo "$EVIDENCE_RESPONSE" | jq .
  fail "Evidence package did not preserve tenant/correlation metadata"
fi

pass "Compliance evidence package contains success and failure evidence"
echo ""
echo "=================================================="
echo " MVP 10 SMOKE TEST PASSED"
echo "=================================================="
echo "Tenant ID:              $TENANT_ID"
echo "Correlation ID:         $CORRELATION_ID"
echo "Document ID:            $DOCUMENT_ID"
echo "Payment ID:             $PAYMENT_ID"
echo "Evidence count:         $EVIDENCE_COUNT"
echo "Success evidence types: $SUCCESS_EVIDENCE_TYPES"
echo "Failure evidence types: $FAILURE_EVIDENCE_TYPES"
echo "=================================================="
