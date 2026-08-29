# MVP 11 Production-Readiness Foundation

Date: 2026-05-24

MVP 11 moves N-AITFS from local runtime validation toward production/SaaS readiness. The current repo is still configured for local/demo execution by default, but runtime endpoints, credentials, and ports are now documented in `.env.example` and should be supplied through environment-specific secret/config management outside local development.

## Readiness Checklist

- Configuration is environment-driven for service ports, gateway route targets, JWT issuer/JWKS, databases, Kafka, MinIO, Qdrant, Keycloak, and tracing.
- Local/demo defaults remain available for developer validation.
- Production secrets must not be committed; use a vault, Kubernetes secrets, cloud secret manager, or equivalent.
- Tenant, correlation, idempotency, and JWT requirements are enforced at the gateway for protected API routes.
- MVP10 smoke validation has proven document, knowledge, payment, and compliance evidence flow through the gateway.
- Docker/Testcontainers integration tests remain separated from default local unit tests.

## Environment Variables

### API Gateway

- `API_GATEWAY_PORT`: Gateway HTTP port. Local default: `8080`.
- `API_GATEWAY_URL`: Base URL used by scripts.
- `JWT_ISSUER_URI`: OIDC issuer URL used to validate JWTs.
- `JWT_JWK_SET_URI`: JWKS endpoint used by the gateway.
- `DOCUMENT_INTELLIGENCE_SERVICE_URL`: Downstream document service base URL.
- `KNOWLEDGE_SERVICE_URL`: Downstream knowledge service base URL.
- `PAYMENT_ORCHESTRATION_SERVICE_URL`: Downstream payment service base URL.
- `COMPLIANCE_EVIDENCE_SERVICE_URL`: Downstream evidence service base URL.

### Payment Orchestration Service

- `PAYMENT_ORCHESTRATION_SERVICE_PORT`: Payment service HTTP port.
- `PAYMENT_DATASOURCE_URL`: JDBC URL for the payment schema database.
- `PAYMENT_DATASOURCE_USERNAME`: Database username.
- `PAYMENT_DATASOURCE_PASSWORD`: Database password.
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers.
- `JWT_ISSUER_URI`: JWT issuer for resource-server validation.
- `APP_SECURITY_ENABLED`: Local compatibility toggle; production should enforce security.
- `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`: OTLP HTTP traces endpoint.

### Compliance Evidence Service

- `COMPLIANCE_EVIDENCE_SERVICE_PORT`: Evidence service HTTP port.
- `COMPLIANCE_DATASOURCE_URL`: JDBC URL for the compliance schema database.
- `COMPLIANCE_DATASOURCE_USERNAME`: Database username.
- `COMPLIANCE_DATASOURCE_PASSWORD`: Database password.
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers.

### Knowledge Service

- `KNOWLEDGE_SERVICE_PORT`: Knowledge service HTTP port.
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers.
- `QDRANT_HOST`, `QDRANT_PORT`: Qdrant host and HTTP port.
- `QDRANT_COLLECTION`: Knowledge vector collection.
- `QDRANT_VECTOR_SIZE`: Embedding vector dimension.
- `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`: Object storage settings.
- `KNOWLEDGE_EMBEDDING_PROVIDER`: Embedding provider, currently `ollama` for local/demo.
- `OLLAMA_BASE_URL`, `OLLAMA_EMBEDDING_MODEL`, `OLLAMA_CHAT_MODEL`: Local model endpoint and model names.
- `KNOWLEDGE_CHUNK_SIZE`, `KNOWLEDGE_CHUNK_OVERLAP`, `KNOWLEDGE_MIN_CHUNK_SIZE`: Chunking controls.
- `KNOWLEDGE_RETRIEVAL_CANDIDATE_LIMIT`, `KNOWLEDGE_RETRIEVAL_VECTOR_WEIGHT`, `KNOWLEDGE_RETRIEVAL_KEYWORD_WEIGHT`: Retrieval controls.
- `KNOWLEDGE_RERANK_TOP_K`, `KNOWLEDGE_RERANK_PHRASE_WEIGHT`, `KNOWLEDGE_RERANK_DENSITY_WEIGHT`, `KNOWLEDGE_RERANK_LENGTH_WEIGHT`: Reranking controls.
- `KNOWLEDGE_GENERATION_MAX_CONTEXT_CHARS`, `KNOWLEDGE_GENERATION_SYSTEM_PROMPT`: Answer generation controls.

### Document Intelligence Service

- `DOCUMENT_INTELLIGENCE_SERVICE_PORT`: Document service HTTP port.
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers.
- `MINIO_ENDPOINT`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_BUCKET`: Object storage settings.
- `KNOWLEDGE_SERVICE_BASE_URL`: Knowledge service URL for downstream indexing/coordination paths.
- `OCR_ENABLED`, `TESSERACT_COMMAND`, `OCR_MIN_EXTRACTED_CHARS`: OCR controls.

### Keycloak

- `KEYCLOAK_URL`: External Keycloak URL used by scripts.
- `KEYCLOAK_PORT`: Local compose port.
- `KEYCLOAK_CONTAINER`: Local Docker container name.
- `KEYCLOAK_ADMIN_URL`: Keycloak admin URL inside the Keycloak container.
- `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD`: Local admin credentials.
- `REALM`: Realm name.
- `CLIENT_ID`: Demo public client ID.
- `USERNAME`, `PASSWORD`: Demo user credentials.
- `USER_EMAIL`, `USER_FIRST_NAME`, `USER_LAST_NAME`: Demo user profile values.
- `CLIENT_SCOPES`: Client scopes provisioned by the repair script.
- `TOKEN_SCOPE`: OAuth scopes requested by demo token helpers.

Production Keycloak should use managed admin credentials, proper client types, role/scope governance, rotation, and issuer URLs that match the public service endpoint.

### Postgres

- `POSTGRES_HOST`, `POSTGRES_PORT`: Database host and port.
- `POSTGRES_DB`: Database name.
- `POSTGRES_USER`, `POSTGRES_PASSWORD`: Database owner/user credentials.
- `PAYMENT_DATASOURCE_*` and `COMPLIANCE_DATASOURCE_*`: Service-specific JDBC credentials.

Production should use managed Postgres or hardened self-hosted Postgres with TLS, backups, PITR, migration controls, and separate least-privilege users per service/schema.

### Kafka

- `KAFKA_BOOTSTRAP_SERVERS`: Bootstrap list used by services.
- `KAFKA_HOST_PORT`, `KAFKA_ADVERTISED_HOST`: Local compose host listener values.

Production should define broker authentication, TLS, topic ACLs, retention, replication factor, schema compatibility strategy, and dead-letter/retry policy.

### Qdrant

- `QDRANT_HOST`, `QDRANT_PORT`: Service connection values.
- `QDRANT_HTTP_PORT`, `QDRANT_GRPC_PORT`: Local compose port mappings.
- `QDRANT_COLLECTION`, `QDRANT_VECTOR_SIZE`: Knowledge index settings.

Production should define collection lifecycle, backups, access control, TLS/network isolation, and embedding model compatibility.

### MinIO

- `MINIO_ENDPOINT`: Object store endpoint.
- `MINIO_API_PORT`, `MINIO_CONSOLE_PORT`: Local compose ports.
- `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`: Local root credentials.
- `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`: Service access credentials.
- `MINIO_BUCKET`: Document storage bucket.

Production should use non-root service credentials, bucket policies, encryption, retention/legal-hold needs, audit logging, and backup/replication.

### Jaeger / OpenTelemetry

- `JAEGER_UI_PORT`: Local Jaeger UI port.
- `OTEL_GRPC_PORT`, `OTEL_HTTP_PORT`: Local collector ports.
- `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`: Trace export endpoint.

Production should centralize traces, metrics, and logs with tenant/correlation metadata while enforcing PII and regulated-data controls.

## Configuration Cleanup Status

- Gateway downstream routes now support environment overrides.
- Gateway, knowledge, payment, compliance, and document service ports have documented environment variables where used.
- Payment OTLP tracing endpoint now supports environment override.
- Demo Keycloak and token scripts now support environment overrides for realm, client, user, scopes, and credentials.
- Docker compose local infrastructure now supports environment overrides for common ports and demo credentials.
- Local test tenant IDs and fixture account values remain in tests and smoke scripts by design.
- Local/demo defaults such as `localhost`, `test-user`, `naitfs-client`, `minioadmin`, and `naitfs_password` are not production credentials.

## Startup Validation Checklist

1. Create a local environment file if needed:

```bash
cp .env.example .env
```

2. Start Docker infrastructure:

```bash
docker compose -f platform/docker/docker-compose.naitfs.yml up -d
```

3. Ensure Keycloak realm/client/user/scopes are ready:

```bash
./scripts/repair-keycloak-demo.sh
```

4. Start Spring Boot services:

```bash
./scripts/start-all-services.sh
```

5. Verify service health:

```bash
./scripts/check-health.sh
```

6. Run the validated MVP10 smoke test:

```bash
./scripts/smoke-test-mvp10.sh
```

## Validated MVP10 Demo Evidence

The latest live MVP10 smoke validation completed successfully against local services on 2026-05-24.

- `tenantId`: `tenant-mvp10-4dd715fb`
- `correlationId`: `corr-4dd715fb-5fd3-4f2f-bcad-98c85c20f233`
- `documentId`: `1779639704575-mvp10.RgMwx1`
- `paymentId`: `efdd4298-0fcc-47b0-ad26-6538d788b17d`
- `evidence count`: `8`
- `success evidence types`: `DOCUMENT_EXTRACTED`, `KNOWLEDGE_ANSWER_GENERATED`, `PAYMENT_COMPLETED`, `PAYMENT_RECEIVED`
- `failure evidence types`: `DOCUMENT_EXTRACTION_FAILED`, `KNOWLEDGE_ANSWER_FAILED`

## Production Gaps To Close After MVP11

- Replace local Keycloak demo realm setup with audited IaC or realm export promotion.
- Enforce production JWT audience/issuer/client policy for every service that accepts direct traffic.
- Introduce per-service database users and secret rotation.
- Add Kafka security: TLS/SASL, ACLs, replication, retention, and backup strategy.
- Add object storage encryption, bucket policy, and retention rules.
- Add centralized metrics/logs/traces with alerting and SLOs.
- Add deployment manifests for the target platform.
- Add CI/CD gates for `mvn test`, integration tests, image scanning, dependency scanning, and smoke validation.
- Define tenant lifecycle, data isolation model, and evidence retention policy.
