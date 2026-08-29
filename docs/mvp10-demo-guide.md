# MVP 10 Demo Guide

MVP 10 demonstrates a gateway-controlled regulated workflow with success and failure evidence:

- document extraction success and failure
- grounded knowledge answer success and failure fallback
- payment workflow evidence
- tenant/correlation propagation
- idempotency replay protection
- gateway header and JWT rejection

## Start Infrastructure

Start the shared infrastructure from the repository root:

```bash
docker compose -f platform/docker/docker-compose.naitfs.yml up -d
```

Key local infrastructure ports:

- Keycloak: `http://localhost:8081`
- Kafka: `localhost:9092`
- PostgreSQL: configured by the compose file
- MinIO/Qdrant/Ollama dependencies: configured by the compose file when present

If the Keycloak demo user or client is missing, repair the local realm:

```bash
./scripts/repair-keycloak-demo.sh
./scripts/repair-keycloak-test-user.sh
```

## Start Services

Start the application services:

```bash
./scripts/start-all-services.sh
```

Expected service ports:

- API Gateway: `http://localhost:8080`
- Document Intelligence Service: `http://localhost:8089`
- Knowledge Service: `http://localhost:8088`
- Payment Orchestration Service: `http://localhost:8082`
- Compliance Evidence Service: `http://localhost:8084`

Check health:

```bash
./scripts/check-health.sh
```

## Run Local Maven Tests

Default verification does not require Docker/Testcontainers:

```bash
mvn test
```

Payment Docker-backed integration tests are excluded from the default Surefire path.

## Run MVP 10 Smoke Test

Run the smoke/demo script:

```bash
./scripts/smoke-test-mvp10.sh
```

The script verifies:

- services and gateway are reachable
- a Keycloak token can be obtained
- document processing works through the gateway
- knowledge answer works through the gateway with `documentId`
- payment request works through the gateway
- compliance evidence can be queried by `correlationId`
- document failure evidence is produced
- knowledge failure evidence is produced
- duplicate idempotency requests do not create duplicate evidence
- the gateway rejects missing tenant/correlation headers
- the gateway rejects an invalid JWT

At completion, the script prints a business-readable evidence summary with tenant ID, correlation ID, document ID, payment ID, evidence count, success evidence types, and failure evidence types.

## Run Docker-Backed Integration Tests

Payment integration tests require Docker because they start PostgreSQL and Kafka with Testcontainers:

```bash
mvn verify -Pintegration-tests
```

Prerequisites:

- Docker daemon running
- current user/session can access Docker
- local or network access to the configured Testcontainers images

For complete verification with integration tests enabled:

```bash
mvn verify -Pfull-verification
```

## Evidence Events

- `DOCUMENT_EXTRACTED`: Document Intelligence extracted text and published `document.extracted.v1`.
- `DOCUMENT_EXTRACTION_FAILED`: Document processing failed, for example because no text could be extracted.
- `KNOWLEDGE_ANSWER_GENERATED`: Knowledge Service generated an answer and citation package.
- `KNOWLEDGE_ANSWER_FAILED`: Knowledge Service could not ground the answer or the model path failed; fallback behavior was used where applicable.
- `PAYMENT_RECEIVED`: Payment Orchestration accepted a payment request.
- `PAYMENT_COMPLETED`: Payment Orchestration completed the payment workflow.
- `PAYMENT_STATUS_CHANGED`: Payment status transition evidence from payment events.
- `PAYMENT_DECISION_EVIDENCE`: Payment decision evidence, including rejected or risk-based outcomes when produced.

Every evidence record should preserve tenant and correlation metadata and include an immutable payload hash.
