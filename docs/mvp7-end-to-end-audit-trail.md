# MVP 7 End-to-End Audit Trail

## Purpose

MVP 7 proves that a payment decision can be traced from the original API request through orchestration, Kafka, compliance evidence capture, and tenant-safe evidence lookup.

The goal is not only to process a payment. The goal is to leave a queryable audit trail that shows:

- Which tenant initiated the request.
- Which correlation ID ties the workflow together.
- Which payment was evaluated.
- What decision was produced.
- What risk level was assigned.
- Which immutable evidence payload was stored.
- That another tenant cannot read the evidence.

## Architecture Flow

```text
Client
  -> API Gateway
  -> Payment Service
  -> Kafka
  -> Compliance Evidence Service
  -> Evidence Query
```

Practical flow:

1. The client sends a payment request through the API Gateway.
2. The API Gateway enforces JWT authentication and tenant headers.
3. The Payment Service receives the request and publishes payment events.
4. Kafka carries the payment decision event.
5. The Compliance Evidence Service consumes the event and stores evidence.
6. The client queries evidence through the API Gateway by `paymentId` or `correlationId`.

## Required Headers

All protected gateway requests require these headers:

```http
Authorization: Bearer <jwt>
X-Tenant-Id: <tenant-id>
X-Correlation-Id: <correlation-id>
```

Header behavior:

- `Authorization` must contain a valid JWT.
- Evidence query endpoints require the `evidence.read` token scope.
- Payment creation requires the `payments.write` token scope.
- `X-Tenant-Id` is required and is used to scope evidence queries.
- `X-Correlation-Id` ties gateway, payment, Kafka, and evidence logs together.

## Evidence Query Endpoints

Evidence is queried through the API Gateway:

```http
GET /api/v1/evidence/payment/{paymentId}
GET /api/v1/evidence/correlation/{correlationId}
```

Examples:

```bash
curl -s \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-001" \
  "http://localhost:8080/api/v1/evidence/payment/$PAYMENT_ID" | jq .
```

```bash
curl -s \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-001" \
  "http://localhost:8080/api/v1/evidence/correlation/$CORRELATION_ID" | jq .
```

## Expected Responses

Successful evidence query:

```json
{
  "count": 1,
  "evidence": [
    {
      "id": "00000000-0000-0000-0000-000000000001",
      "tenantId": "tenant-demo-001",
      "correlationId": "corr-demo-001",
      "documentId": null,
      "paymentId": "pay-123",
      "evidenceType": "PAYMENT_DECISION_EVIDENCE",
      "sourceService": "payment-orchestration-service",
      "sourceTopic": "payment.processed",
      "eventKey": "pay-123",
      "summary": "Payment decision evidence recorded for paymentId=pay-123, customerId=customer-123, decision=COMPLETED, riskLevel=MEDIUM, amount=2000000.99, currency=USD",
      "riskLevel": "MEDIUM",
      "decision": "COMPLETED",
      "actor": "customer-123",
      "payloadHash": "sha256-hash",
      "evidencePayload": "{\"paymentId\":\"pay-123\"}",
      "createdAt": "2026-05-12T00:00:00Z",
      "receivedAt": "2026-05-12T00:00:01Z",
      "immutableRecord": true
    }
  ]
}
```

Wrong-tenant query:

```json
{
  "count": 0,
  "evidence": []
}
```

Missing tenant header:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Missing required header: X-Tenant-Id"
}
```

Missing or invalid token:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid bearer token"
}
```

## Tenant Isolation Behavior

Evidence queries are tenant-scoped.

For these endpoints:

```http
GET /api/v1/evidence/payment/{paymentId}
GET /api/v1/evidence/correlation/{correlationId}
```

The Compliance Evidence Service queries by both tenant and business identifier:

- `tenantId + paymentId`
- `tenantId + correlationId`

Expected behavior:

- Correct tenant receives matching evidence.
- Wrong tenant receives `count: 0` and `evidence: []`.
- Missing `X-Tenant-Id` is rejected.
- A valid JWT without `evidence.read` is rejected by the gateway.
- Unauthenticated access is rejected by the gateway.

## Smoke Test

Run the MVP 7 smoke test from the repository root:

```bash
./scripts/smoke-test-mvp7.sh
```

The smoke test:

- Gets a JWT from local Keycloak.
- Creates a unique tenant and correlation ID.
- Posts a payment through the API Gateway.
- Waits for Kafka and evidence processing.
- Queries evidence by `paymentId`.
- Queries evidence by `correlationId`.
- Verifies evidence has tenant ID, payment ID, decision, and risk level.
- Verifies a wrong tenant cannot see the evidence.

## Enterprise Readiness

MVP 7 demonstrates several controls expected in regulated enterprise systems:

- Authenticated access through a central gateway.
- Scope-based authorization for evidence reads.
- Mandatory tenant context on protected requests.
- Correlation ID propagation for traceability.
- Kafka-backed audit trail capture.
- Immutable evidence records with payload hashes.
- Queryable compliance evidence by payment and correlation ID.
- Tenant isolation on evidence lookup.
- Negative verification that cross-tenant evidence disclosure is blocked.

Together, these controls show that payment decisions are not just operational events. They become tenant-safe, auditable evidence that can support compliance review, incident investigation, and regulated enterprise reporting.
