# MVP13 Observability Dashboard Guide

MVP13 adds dashboard-ready compliance evidence summaries for the regulated trust flow. The endpoints are intended for demo screens, executive status views, and quick validation without reading service logs.

## Prerequisites

Start the NAITFS platform and obtain a valid gateway token as described in the MVP11 and MVP12 guides.

Common variables:

```bash
export GATEWAY_URL=http://localhost:8080
export TENANT_ID=tenant-demo
export CORRELATION_ID=corr-demo-mvp13
```

## Correlation Summary

```bash
curl -sS "$GATEWAY_URL/api/v1/evidence/correlation/$CORRELATION_ID/summary" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: $TENANT_ID" | jq .
```

Response fields:

```json
{
  "tenantId": "tenant-demo",
  "correlationId": "corr-demo-mvp13",
  "totalEvidence": 4,
  "evidenceTypes": [
    "DOCUMENT_EXTRACTED",
    "KNOWLEDGE_ANSWER_GENERATED",
    "PAYMENT_COMPLETED",
    "PAYMENT_RECEIVED"
  ],
  "sourceServices": [
    "document-intelligence-service",
    "knowledge-service",
    "payment-orchestration-service"
  ],
  "firstEventTime": "2026-05-25T15:00:00Z",
  "lastEventTime": "2026-05-25T15:01:30Z",
  "status": "COMPLETE"
}
```

The response also keeps the MVP11 fields `totalEvidenceCount`, `successfulControls`, `failedControls`, `servicesInvolved`, `auditStatus`, and `riskStatus` so existing demo scripts continue to work.

Status values:

- `NO_EVIDENCE`: no tenant-scoped records exist for the correlation.
- `INCOMPLETE`: records exist, but required trust-flow controls are missing.
- `COMPLETE`: document extraction, grounded knowledge, payment received, and payment completed evidence are present with no failure evidence.
- `REVIEW_REQUIRED`: failure evidence is present.

## Tenant Summary

```bash
curl -sS "$GATEWAY_URL/api/v1/evidence/tenant/$TENANT_ID/summary" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Response fields:

```json
{
  "tenantId": "tenant-demo",
  "totalEvidence": 12,
  "totalCorrelations": 3,
  "evidenceCountByType": {
    "DOCUMENT_EXTRACTED": 3,
    "KNOWLEDGE_ANSWER_GENERATED": 3,
    "PAYMENT_RECEIVED": 3,
    "PAYMENT_COMPLETED": 3
  },
  "latestCorrelationIds": [
    "corr-demo-mvp13",
    "corr-demo-mvp12",
    "corr-demo-mvp11"
  ],
  "latestEventTime": "2026-05-25T15:01:30Z"
}
```

## Demo Dashboard

```bash
curl -sS "$GATEWAY_URL/api/v1/evidence/demo-dashboard?tenantId=$TENANT_ID" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Response fields:

```json
{
  "tenantId": "tenant-demo",
  "serviceName": "compliance-evidence-service",
  "generatedAt": "2026-05-25T15:02:00Z",
  "totals": {
    "totalEvidence": 12,
    "totalCorrelations": 3,
    "latestEventTime": "2026-05-25T15:01:30Z"
  },
  "evidenceByType": {
    "DOCUMENT_EXTRACTED": 3,
    "KNOWLEDGE_ANSWER_GENERATED": 3,
    "PAYMENT_RECEIVED": 3,
    "PAYMENT_COMPLETED": 3
  },
  "recentTrustFlows": [
    {
      "correlationId": "corr-demo-mvp13",
      "totalEvidence": 4,
      "evidenceTypes": [
        "DOCUMENT_EXTRACTED",
        "KNOWLEDGE_ANSWER_GENERATED",
        "PAYMENT_COMPLETED",
        "PAYMENT_RECEIVED"
      ],
      "sourceServices": [
        "document-intelligence-service",
        "knowledge-service",
        "payment-orchestration-service"
      ],
      "lastEventTime": "2026-05-25T15:01:30Z",
      "status": "COMPLETE"
    }
  ],
  "readinessStatus": "READY"
}
```

`readinessStatus` is `READY` when the tenant has demo evidence and at least one correlation. It is `NO_DEMO_DATA` for an empty tenant.

## Verification

Run the tests:

```bash
mvn test
```

MVP13 does not change the MVP11 shell scripts or the MVP12 Newman runner. Existing MVP11 and MVP12 demo flows can continue to use their current commands.
