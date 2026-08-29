# MVP20 Enterprise Document Intelligence and Knowledge Workflow

MVP20 implements the enterprise workflow layer for NAITFS document intelligence and knowledge retrieval.

## Document Intelligence Additions

Document processing now feeds an enterprise workflow state in addition to the existing document record and Kafka event path.

Implemented controls:

- Lifecycle states: `RECEIVED`, `CLASSIFIED`, `PARSED`, `ENRICHED`, `IN_REVIEW`, `APPROVED`, `PUBLISHED`, `DEPRECATED`, `QUARANTINED`, and `FAILED`.
- Classification and sensitivity gates for public, internal, confidential, restricted, low-confidence, DLP, and prompt-injection indicators.
- Review tasks for sensitive, low-confidence, failed, quarantined, and feedback-driven documents.
- Review decisions for approve, reject, quarantine, deprecate, redact, merge, and split.
- Publish blocking for failed or quarantined documents.
- Feedback capture that creates curator review work.
- Searchable audit events by tenant, document, user, action, and date range.
- Operations metrics for lifecycle counts, review backlog, quarantines, and feedback.

New document endpoints:

```text
GET  /api/v1/documents/enterprise/{documentId}/workflow
GET  /api/v1/documents/enterprise/reviews?tenantId=&status=&riskLevel=&limit=
POST /api/v1/documents/enterprise/{documentId}/reviews
POST /api/v1/documents/enterprise/{documentId}/publish?actorId=
POST /api/v1/documents/enterprise/{documentId}/feedback
GET  /api/v1/documents/enterprise/audit?tenantId=&documentId=&userId=&action=&from=&to=&limit=
GET  /api/v1/documents/enterprise/metrics
```

## Knowledge Workflow Additions

Knowledge answers now run through runtime governance before generation.

Implemented controls:

- Permission-aware retrieval using optional `authorizedDocumentIds` and caller roles.
- Prompt-injection blocking for user queries and retrieved document evidence.
- DLP blocking for retrieved evidence unless `sensitivityClearance` is `HIGH`.
- Retrieval events with tenant, workspace, user, document, status, blocked reason, citation count, confidence, latency, and timestamp.
- Feedback capture linked to retrieval events and documents.
- Evaluation summary for grounded, blocked, no-evidence, grounding rate, block rate, and average confidence.
- Operations metrics for retrieval and feedback volume.

Extended knowledge answer request fields:

```json
{
  "query": "Which policy requires sanctions screening?",
  "documentId": "doc-123",
  "tenantId": "tenant-a",
  "workspaceId": "workspace-risk",
  "userId": "analyst-1",
  "roles": ["knowledge.read"],
  "authorizedDocumentIds": ["doc-123"],
  "sensitivityClearance": "LOW"
}
```

Existing callers can continue sending only `query` and `documentId`.

New knowledge endpoints:

```text
POST /api/v1/knowledge/feedback
GET  /api/v1/knowledge/retrieval-events?tenantId=&status=&limit=
POST /api/v1/knowledge/evaluation/run
GET  /api/v1/knowledge/operations/metrics
```

## Verification

Run the backend suite:

```bash
mvn test
```

Run the affected services only:

```bash
mvn -pl services/document-intelligence-service,services/knowledge-service test
```

Run the TrustFlow Console build:

```bash
cd web/trustflow-console
npm install
npm run build
```

Verified locally on July 1, 2026:

- `mvn -pl services/document-intelligence-service,services/knowledge-service test` passed.
- `mvn test` passed across all backend modules.
- `npm run build` passed for the TrustFlow Console.
