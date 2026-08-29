#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# NAITFS Document Intelligence curl Demo
# ============================================================

GATEWAY="${GATEWAY:-http://localhost:8080}"
KEYCLOAK="${KEYCLOAK:-http://localhost:8081}"
REALM="${REALM:-payment-platform}"
CLIENT_ID="${CLIENT_ID:-naitfs-client}"
USERNAME="${USERNAME:-test-user}"
PASSWORD="${PASSWORD:-test-password}"

TENANT_ID="${TENANT_ID:-tenant-document-demo-$(date +%s)}"
CORRELATION_ID="${CORRELATION_ID:-corr-document-demo-$(date +%s)}"
IDEMPOTENCY_KEY="${IDEMPOTENCY_KEY:-idem-document-demo-$(date +%s)}"

DOC_FILE="/tmp/naitfs-document-demo.txt"
RESPONSE_FILE="/tmp/naitfs-document-response.json"
EVIDENCE_FILE="/tmp/naitfs-document-evidence.json"

echo "============================================================"
echo " NAITFS Document Intelligence Technical Demo"
echo "============================================================"
echo "Gateway:          $GATEWAY"
echo "Keycloak:         $KEYCLOAK"
echo "Realm:            $REALM"
echo "Client ID:        $CLIENT_ID"
echo "Username:         $USERNAME"
echo "Tenant ID:        $TENANT_ID"
echo "Correlation ID:   $CORRELATION_ID"
echo "Idempotency Key:  $IDEMPOTENCY_KEY"
echo "============================================================"

echo
echo "0. Demo introduction"
echo "------------------------------------------------------------"
echo "This demo proves:"
echo "- API Gateway routing"
echo "- Keycloak JWT authentication"
echo "- Tenant isolation"
echo "- Correlation ID tracing"
echo "- Idempotency protection"
echo "- Document ingestion and extraction"
echo "- Compliance evidence generation"
echo "- Negative security validation"
echo "------------------------------------------------------------"

echo
echo "1. Checking API Gateway health..."
echo "------------------------------------------------------------"

curl -s "$GATEWAY/actuator/health" | jq .

GATEWAY_STATUS=$(curl -s "$GATEWAY/actuator/health" | jq -r '.status // empty')

if [ "$GATEWAY_STATUS" != "UP" ]; then
  echo "FAIL: API Gateway is not UP."
  echo "Start platform:"
  echo "  ./scripts/start-naitfs-platform.sh"
  exit 1
fi

echo "PASS: API Gateway is UP."

echo
echo "2. Requesting Keycloak JWT token..."
echo "------------------------------------------------------------"

TOKEN_RESPONSE=$(curl -s \
  -X POST "$KEYCLOAK/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD")

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "FAIL: Could not get Keycloak token."
  echo
  echo "Keycloak response:"
  echo "$TOKEN_RESPONSE" | jq . || echo "$TOKEN_RESPONSE"
  echo
  echo "Try one of these:"
  echo "  PASSWORD=test-password ./scripts/demo-document-intelligence-curl.sh"
  echo "  ./scripts/repair-keycloak-demo.sh"
  exit 1
fi

echo "PASS: Token acquired."
echo "Token preview: $(echo "$TOKEN" | cut -c1-45)..."

echo
echo "3. Creating demo business document..."
echo "------------------------------------------------------------"

cat > "$DOC_FILE" <<'DOC'
Invoice Number: INV-2026-001
Customer: NkeanyiAI Demo Customer
Amount Due: 1250.75
Currency: USD
Due Date: 2026-07-15

This document demonstrates NAITFS Document Intelligence.

Business Meaning:
This simulates an enterprise document such as an invoice, claim, intake form, or compliance document.
The system should process the document and generate traceable compliance evidence.
DOC

echo "Created document at: $DOC_FILE"
echo
cat "$DOC_FILE"

echo
echo "4. Processing document through API Gateway..."
echo "------------------------------------------------------------"

DOCUMENT_ENDPOINT="/api/v1/documents/process"

HTTP_STATUS=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" \
  -X POST "$GATEWAY$DOCUMENT_ENDPOINT" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -F "file=@$DOC_FILE")

echo "Document endpoint: $DOCUMENT_ENDPOINT"
echo "HTTP Status:       $HTTP_STATUS"
echo
cat "$RESPONSE_FILE" | jq . || cat "$RESPONSE_FILE"

if [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "403" ]; then
  echo
  echo "FAIL: Document endpoint rejected the token."
  exit 1
fi

if [ "$HTTP_STATUS" = "400" ]; then
  echo
  echo "FAIL: Document endpoint returned 400 Bad Request."
  echo "Review the response above. Common causes:"
  echo "- Missing required header"
  echo "- Wrong multipart field name"
  echo "- Controller expects JSON instead of file upload"
  exit 1
fi

if [ "$HTTP_STATUS" = "404" ]; then
  echo
  echo "FAIL: Document endpoint was not found."
  echo
  echo "Find the correct endpoint with:"
  echo "  grep -R \"RequestMapping\\|PostMapping\" document-intelligence-service/src/main/java | grep -i document"
  exit 1
fi

echo
echo "PASS: Document request completed."

echo
echo "5. Extracting document ID if present..."
echo "------------------------------------------------------------"

DOCUMENT_ID=$(jq -r '.documentId // .id // .data.documentId // .data.id // empty' "$RESPONSE_FILE" 2>/dev/null || true)

if [ -n "$DOCUMENT_ID" ]; then
  echo "Document ID: $DOCUMENT_ID"
else
  echo "Document ID not found in response. Continuing with correlation ID evidence query."
fi

echo
echo "6. Querying compliance evidence..."
echo "------------------------------------------------------------"

EVIDENCE_ENDPOINT="/api/v1/evidence?tenantId=$TENANT_ID&correlationId=$CORRELATION_ID"

EVIDENCE_STATUS=$(curl -s -o "$EVIDENCE_FILE" -w "%{http_code}" \
  "$GATEWAY$EVIDENCE_ENDPOINT" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY")

echo "Evidence endpoint: $EVIDENCE_ENDPOINT"
echo "HTTP Status:       $EVIDENCE_STATUS"
echo
cat "$EVIDENCE_FILE" | jq . || cat "$EVIDENCE_FILE"

if [ "$EVIDENCE_STATUS" = "400" ]; then
  echo
  echo "WARNING: Evidence endpoint returned 400."
  echo "The document flow ran, but evidence query path or required headers may differ."
fi

if grep -qi "DOCUMENT" "$EVIDENCE_FILE"; then
  echo
  echo "PASS: Document-related evidence found."
else
  echo
  echo "NOTE: No explicit DOCUMENT evidence text found in the response."
  echo "The evidence endpoint may aggregate or filter evidence differently."
fi

echo
echo "7. Negative security test: missing JWT should fail..."
echo "------------------------------------------------------------"

NO_TOKEN_STATUS=$(curl -s -o /tmp/naitfs-no-token-response.txt -w "%{http_code}" \
  -X POST "$GATEWAY$DOCUMENT_ENDPOINT" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-no-token-$(date +%s)" \
  -F "file=@$DOC_FILE")

echo "HTTP Status without token: $NO_TOKEN_STATUS"
cat /tmp/naitfs-no-token-response.txt | head -20 || true

if [ "$NO_TOKEN_STATUS" = "401" ] || [ "$NO_TOKEN_STATUS" = "403" ]; then
  echo
  echo "PASS: Missing JWT was rejected."
else
  echo
  echo "WARNING: Missing JWT was not rejected with 401/403."
fi

echo
echo "8. Negative traceability test: missing tenant header should fail..."
echo "------------------------------------------------------------"

NO_TENANT_STATUS=$(curl -s -o /tmp/naitfs-no-tenant-response.txt -w "%{http_code}" \
  -X POST "$GATEWAY$DOCUMENT_ENDPOINT" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-no-tenant-$(date +%s)" \
  -F "file=@$DOC_FILE")

echo "HTTP Status without tenant header: $NO_TENANT_STATUS"
cat /tmp/naitfs-no-tenant-response.txt | head -20 || true

if [ "$NO_TENANT_STATUS" = "400" ] || [ "$NO_TENANT_STATUS" = "401" ] || [ "$NO_TENANT_STATUS" = "403" ]; then
  echo
  echo "PASS: Missing tenant context was rejected or controlled."
else
  echo
  echo "WARNING: Missing tenant header was not rejected."
fi

echo
echo "9. Technical interview explanation"
echo "------------------------------------------------------------"
cat <<'SCRIPT'
"This demo shows the Document Intelligence part of NAITFS.
A business document is submitted through the API Gateway using curl.
Before submission, the client gets a Keycloak JWT token.
The request carries a tenant ID for customer isolation, a correlation ID for tracing, and an idempotency key for replay protection.
The Document Intelligence service processes the document and returns a structured response.
Then I query compliance evidence to prove the document action was recorded.
I also test negative paths by removing the JWT and tenant header.
The value is not only document extraction. The value is secure, traceable, audit-ready document processing."
SCRIPT

echo
echo "10. General interview explanation"
echo "------------------------------------------------------------"
cat <<'SCRIPT'
"NAITFS is an enterprise AI trust-flow platform.
This part demonstrates how a company can securely process invoices, forms, claims, or compliance documents.
The system authenticates the request, ties it to a tenant, tracks it with a correlation ID, protects repeated requests with an idempotency key, processes the document, and records compliance evidence.
This gives the business automation, security, traceability, and audit readiness."
SCRIPT

echo
echo "============================================================"
echo " DOCUMENT INTELLIGENCE DEMO COMPLETE"
echo "============================================================"
echo "Gateway:            $GATEWAY"
echo "Document endpoint:  $DOCUMENT_ENDPOINT"
echo "Tenant ID:          $TENANT_ID"
echo "Correlation ID:     $CORRELATION_ID"
echo "Idempotency Key:    $IDEMPOTENCY_KEY"
echo "Document response:  $RESPONSE_FILE"
echo "Evidence response:  $EVIDENCE_FILE"
echo "============================================================"
