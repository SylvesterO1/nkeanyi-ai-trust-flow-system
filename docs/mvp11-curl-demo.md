# MVP11 Curl Demo

Set common variables:

```bash
export GATEWAY_URL=http://localhost:8080
export KEYCLOAK_URL=http://localhost:8081
export REALM=payment-platform
export CLIENT_ID=naitfs-client
export USERNAME=test-user
export PASSWORD=test-password
export TENANT_ID=tenant-mvp11-manual
export CORRELATION_ID=corr-mvp11-manual-$(uuidgen | tr '[:upper:]' '[:lower:]')
```

## Get Keycloak Token

```bash
export TOKEN=$(curl -sS -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" \
  -d "scope=openid profile email documents.write knowledge.read payments.write payments.read evidence.read" \
  | jq -r '.access_token')
```

## Document Process

```bash
printf '%s\n' 'Vendor activation requires sanctions screening before payment release.' > /tmp/mvp11-control.txt

curl -sS -X POST "$GATEWAY_URL/api/v1/documents/process" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-doc-manual" \
  -F "file=@/tmp/mvp11-control.txt;type=text/plain" | jq .
```

Save `.objectName` as `DOCUMENT_ID`.

## Knowledge Answer

```bash
export DOCUMENT_ID=<objectName-from-document-response>

curl -sS -X POST "$GATEWAY_URL/api/v1/knowledge/answer" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-knowledge-manual" \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"What is required before payment release?\",\"documentId\":\"$DOCUMENT_ID\"}" | jq .
```

## Create Payment

```bash
curl -sS -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-payment-manual" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"customer-mvp11-manual",
    "sourceAccount":"ACC-MVP11-SOURCE",
    "destinationAccount":"ACC-MVP11-DEST",
    "amount":1250.75,
    "currency":"USD",
    "paymentReference":"MVP11-MANUAL",
    "paymentMethod":"BANK_TRANSFER",
    "narration":"Manual MVP11 demo"
  }' | jq .
```

## Evidence Lookup

```bash
curl -sS "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-evidence-manual" | jq .
```

## Evidence Summary

```bash
curl -sS "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID/summary" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-summary-manual" | jq .
```

## Trust Certificate

```bash
curl -sS "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID/certificate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-certificate-manual" | jq .
```

## Negative Security Tests

Missing tenant header:

```bash
curl -i -sS "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-missing-tenant"
```

Invalid JWT:

```bash
curl -i -sS "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID" \
  -H "Authorization: Bearer invalid.jwt.token" \
  -H "X-Tenant-Id: $TENANT_ID" \
  -H "X-Correlation-Id: $CORRELATION_ID" \
  -H "Idempotency-Key: idem-mvp11-invalid-jwt"
```
