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
  file="$(mktemp "${TMPDIR:-/tmp}/mvp11.XXXXXX")"
  TMP_FILES+=("$file")
  echo "$file"
}

json_get() {
  jq -r "$1 // empty" "$2"
}

http_health() {
  local name="$1"
  local url="$2"
  local out status
  out="$(mktemp_track)"
  status="$(curl -sS -o "$out" -w "%{http_code}" "$url/actuator/health" || true)"
  if [ "$status" != "200" ] || ! jq -e '.status == "UP"' "$out" >/dev/null 2>&1; then
    cat "$out" 2>/dev/null || true
    fail "$name did not report UP at $url/actuator/health"
  fi
  pass "$name is UP"
}

call_json_status() {
  local method="$1"
  local url="$2"
  local idempotency_key="$3"
  local body="$4"
  local out="$5"

  curl -sS -o "$out" -w "%{http_code}" \
    -X "$method" "$url" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-Tenant-Id: $TENANT_ID" \
    -H "X-Correlation-Id: $CORRELATION_ID" \
    -H "Idempotency-Key: $idempotency_key" \
    -H "Content-Type: application/json" \
    -d "$body"
}

gateway_get() {
  local url="$1"
  local out="$2"
  curl -sS -o "$out" -w "%{http_code}" \
    "$url" \
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
  local evidence_type="$2"
  echo "$evidence_json" | jq -r --arg type "$evidence_type" '[.evidence[]? | select(.evidenceType == $type)] | length'
}

wait_for_evidence_types() {
  local label="$1"
  shift
  local evidence_json=""
  local deadline=$((SECONDS + EVIDENCE_WAIT_SECONDS))

  while [ "$SECONDS" -le "$deadline" ]; do
    evidence_json="$(query_evidence_json)"
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

echo "============================================================"
echo " NAITFS MVP11 Regulated Trust Flow Demo"
echo "============================================================"
echo "Gateway: $GATEWAY_URL"
echo

http_health "api-gateway" "$GATEWAY_URL"
http_health "document-intelligence-service" "$DOCUMENT_SERVICE_URL"
http_health "knowledge-service" "$KNOWLEDGE_SERVICE_URL"
http_health "payment-orchestration-service" "$PAYMENT_SERVICE_URL"
http_health "compliance-evidence-service" "$COMPLIANCE_SERVICE_URL"
echo

echo "Obtaining Keycloak demo token..."
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
pass "JWT token obtained"
echo

RUN_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
TENANT_ID="tenant-mvp11-${RUN_ID:0:8}"
CORRELATION_ID="corr-mvp11-$RUN_ID"
DOC_IDEM="idem-mvp11-doc-$RUN_ID"
KNOWLEDGE_IDEM="idem-mvp11-knowledge-$RUN_ID"
PAYMENT_IDEM="idem-mvp11-payment-$RUN_ID"
DOCUMENT_ID=""
PAYMENT_ID=""

DOC_FILE="$(mktemp_track)"
cat > "$DOC_FILE" <<'DOC'
NAITFS Regulated Vendor Onboarding Control

Before account activation, the platform must complete sanctions screening, preserve the tenant ID, maintain a correlation ID, and retain immutable evidence across document, knowledge, payment, and compliance workflows.
DOC

echo "Tenant ID:      $TENANT_ID"
echo "Correlation ID: $CORRELATION_ID"
echo

echo "Verifying security controls..."
MISSING_HEADER_RESPONSE="$(mktemp_track)"
MISSING_HEADER_STATUS="$(curl -sS -o "$MISSING_HEADER_RESPONSE" -w "%{http_code}" \
  "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: missing-tenant-$RUN_ID" || true)"
[ "$MISSING_HEADER_STATUS" = "400" ] || fail "Expected missing tenant header rejection HTTP 400, got $MISSING_HEADER_STATUS"
pass "Missing tenant header rejected"

INVALID_JWT_RESPONSE="$(mktemp_track)"
INVALID_JWT_STATUS="$(curl -sS -o "$INVALID_JWT_RESPONSE" -w "%{http_code}" \
  "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID" \
  -H "Authorization: Bearer invalid.jwt.token" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: invalid-jwt-$RUN_ID" || true)"
[ "$INVALID_JWT_STATUS" = "401" ] || fail "Expected invalid JWT rejection HTTP 401, got $INVALID_JWT_STATUS"
pass "Invalid JWT rejected"
echo

echo "Processing regulated document..."
DOC_RESPONSE="$(mktemp_track)"
DOC_STATUS="$(curl -sS -o "$DOC_RESPONSE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/v1/documents/process" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $DOC_IDEM" \
  -F "file=@$DOC_FILE;filename=mvp11-control.txt;type=text/plain")"
[ "$DOC_STATUS" = "200" ] || { cat "$DOC_RESPONSE" | jq . 2>/dev/null || cat "$DOC_RESPONSE"; fail "Document processing returned HTTP $DOC_STATUS"; }
DOCUMENT_ID="$(json_get '.objectName' "$DOC_RESPONSE")"
[ -n "$DOCUMENT_ID" ] || fail "Document response did not include objectName"
pass "Document extraction verified: $DOCUMENT_ID"

wait_for_evidence_types "document extraction" DOCUMENT_EXTRACTED >/dev/null
pass "Document extraction evidence recorded"
echo

echo "Generating grounded knowledge answer..."
ANSWER_BODY="$(jq -n \
  --arg query "What must happen before account activation?" \
  --arg documentId "$DOCUMENT_ID" \
  '{query: $query, documentId: $documentId}')"
ANSWER_RESPONSE="$(mktemp_track)"
ANSWER_STATUS="$(call_json_status POST "$GATEWAY_URL/api/v1/knowledge/answer" "$KNOWLEDGE_IDEM" "$ANSWER_BODY" "$ANSWER_RESPONSE")"
[ "$ANSWER_STATUS" = "200" ] || { cat "$ANSWER_RESPONSE" | jq . 2>/dev/null || cat "$ANSWER_RESPONSE"; fail "Knowledge answer returned HTTP $ANSWER_STATUS"; }
jq -e '.citationPackage.citations | length >= 1' "$ANSWER_RESPONSE" >/dev/null || fail "Knowledge answer did not include citations"
pass "Knowledge answer generation verified"

wait_for_evidence_types "knowledge answer" DOCUMENT_EXTRACTED KNOWLEDGE_ANSWER_GENERATED >/dev/null
pass "Knowledge answer evidence recorded"
echo

echo "Creating regulated payment..."
PAYMENT_BODY="$(jq -n \
  --arg customerId "customer-mvp11-${RUN_ID:0:8}" \
  --arg sourceAccount "ACC-MVP11-SOURCE" \
  --arg destinationAccount "ACC-MVP11-DEST" \
  --arg paymentReference "MVP11-$RUN_ID" \
  '{customerId: $customerId, sourceAccount: $sourceAccount, destinationAccount: $destinationAccount, amount: 1250.75, currency: "USD", paymentReference: $paymentReference, paymentMethod: "BANK_TRANSFER", narration: "MVP11 regulated trust-flow demo"}')"
PAYMENT_RESPONSE="$(mktemp_track)"
PAYMENT_STATUS="$(call_json_status POST "$GATEWAY_URL/api/v1/payments" "$PAYMENT_IDEM" "$PAYMENT_BODY" "$PAYMENT_RESPONSE")"
if [ "$PAYMENT_STATUS" != "201" ] && [ "$PAYMENT_STATUS" != "200" ]; then
  cat "$PAYMENT_RESPONSE" | jq . 2>/dev/null || cat "$PAYMENT_RESPONSE"
  fail "Payment request returned HTTP $PAYMENT_STATUS"
fi
PAYMENT_ID="$(json_get '.paymentId' "$PAYMENT_RESPONSE")"
[ -n "$PAYMENT_ID" ] || fail "Payment response did not include paymentId"
pass "Payment received: $PAYMENT_ID"

DUP_PAYMENT_RESPONSE="$(mktemp_track)"
DUP_PAYMENT_STATUS="$(call_json_status POST "$GATEWAY_URL/api/v1/payments" "$PAYMENT_IDEM" "$PAYMENT_BODY" "$DUP_PAYMENT_RESPONSE")"
[ "$DUP_PAYMENT_STATUS" = "200" ] || fail "Expected duplicate idempotency replay HTTP 200, got $DUP_PAYMENT_STATUS"
DUP_PAYMENT_ID="$(json_get '.paymentId' "$DUP_PAYMENT_RESPONSE")"
[ "$DUP_PAYMENT_ID" = "$PAYMENT_ID" ] || fail "Duplicate idempotency replay returned a different paymentId"
pass "Duplicate idempotency behavior verified"
echo

EVIDENCE_RESPONSE="$(wait_for_evidence_types \
  "complete regulated trust flow" \
  DOCUMENT_EXTRACTED \
  KNOWLEDGE_ANSWER_GENERATED \
  PAYMENT_RECEIVED \
  PAYMENT_COMPLETED)"
EVIDENCE_COUNT="$(echo "$EVIDENCE_RESPONSE" | jq -r '.count // (.evidence | length)')"
pass "Compliance evidence records verified"

SUMMARY_RESPONSE="$(mktemp_track)"
SUMMARY_STATUS="$(gateway_get "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID/summary" "$SUMMARY_RESPONSE")"
[ "$SUMMARY_STATUS" = "200" ] || fail "Evidence summary returned HTTP $SUMMARY_STATUS"
jq -e '.auditStatus == "COMPLETE"' "$SUMMARY_RESPONSE" >/dev/null || { cat "$SUMMARY_RESPONSE" | jq .; fail "Evidence summary did not return COMPLETE"; }
pass "Evidence summary auditStatus COMPLETE"

CERTIFICATE_RESPONSE="$(mktemp_track)"
CERTIFICATE_STATUS="$(gateway_get "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID/certificate" "$CERTIFICATE_RESPONSE")"
[ "$CERTIFICATE_STATUS" = "200" ] || fail "Trust certificate returned HTTP $CERTIFICATE_STATUS"
jq -e '.workflowStatus == "VERIFIED"' "$CERTIFICATE_RESPONSE" >/dev/null || { cat "$CERTIFICATE_RESPONSE" | jq .; fail "Trust certificate did not return VERIFIED"; }
pass "Trust certificate VERIFIED"
echo

SUMMARY_AUDIT_STATUS="$(json_get '.auditStatus' "$SUMMARY_RESPONSE")"
SUMMARY_RISK_STATUS="$(json_get '.riskStatus' "$SUMMARY_RESPONSE")"
CERTIFICATE_STATUS_VALUE="$(json_get '.workflowStatus' "$CERTIFICATE_RESPONSE")"

cat <<CERT
============================================================
 NAITFS Regulated Trust Flow Certificate
============================================================
Certificate Type:     NAITFS_REGULATED_TRUST_FLOW
Tenant ID:            $TENANT_ID
Correlation ID:       $CORRELATION_ID
Document ID:          $DOCUMENT_ID
Payment ID:           $PAYMENT_ID
Evidence Count:       $EVIDENCE_COUNT
Audit Status:         $SUMMARY_AUDIT_STATUS
Risk Status:          $SUMMARY_RISK_STATUS
Workflow Status:      $CERTIFICATE_STATUS_VALUE

READY FOR DEMO: NAITFS verified document extraction, grounded
knowledge generation, payment orchestration, compliance evidence,
idempotency replay, tenant isolation, and JWT enforcement.
============================================================
CERT
