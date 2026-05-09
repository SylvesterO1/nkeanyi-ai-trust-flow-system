# MVP 5 Final Report

MVP 5 was completed successfully and committed.

- Commit: `80d87ef`
- Commit message: `Implement MVP 5 gateway security enforcement`
- Primary module: `services/api-gateway`

## Summary

MVP 5 adds gateway-level tenant, correlation, and JWT security enforcement.

Completed capabilities:

- Enforce `X-Tenant-Id` on protected routes.
- Validate tenant ID format.
- Allow `/actuator/health` and `/actuator/info` without tenant headers.
- Auto-generate `X-Correlation-Id` when missing.
- Propagate `X-Tenant-Id` downstream.
- Propagate `X-Correlation-Id` downstream.
- Add trusted downstream header `X-Request-Source: api-gateway`.
- Reject protected requests without bearer token.
- Return `401 Unauthorized` for missing or invalid token.
- Return `403 Forbidden` for valid token with wrong scope.
- Return JSON error responses for rejected requests.
- Add audit-ready logs for accepted and rejected requests.
- Configure OAuth2 resource server JWT validation.
- Configure Keycloak issuer/JWKS properties.
- Extract scopes from JWT `scope` claim.
- Extract scopes from JWT `scp` claim.
- Extract Keycloak realm roles.
- Extract Keycloak client/resource roles.
- Enforce route-level scopes for payments, evidence, documents, and knowledge APIs.
- Add gateway security and tenant/correlation tests.

## Files Changed

- `services/api-gateway/pom.xml`
- `services/api-gateway/src/main/resources/application.yml`
- `services/api-gateway/src/main/java/com/nkeanyi/gateway/filter/TenantCorrelationGatewayFilter.java`
- `services/api-gateway/src/main/java/com/nkeanyi/gateway/security/GatewaySecurityConfig.java`
- `services/api-gateway/src/test/java/com/nkeanyi/gateway/filter/TenantCorrelationGatewayFilterTest.java`
- `services/api-gateway/src/test/java/com/nkeanyi/gateway/security/GatewaySecurityConfigTest.java`

Removed duplicate legacy filter:

- `services/api-gateway/src/main/java/com/nkeanyi/gateway/filter/CorrelationIdFilter.java`

## Commands Run

### Project Discovery

```bash
pwd && rg --files -g 'pom.xml' -g '*Gateway*' -g '*gateway*' -g '*Security*' -g '*Filter*' -g '*Test*'
```

Why it was run: To locate the gateway module, Maven files, security classes, filters, and tests.

Problem solved: Identified `services/api-gateway` and existing partial gateway/security work.

```bash
git status --short
```

Why it was run: To check for uncommitted work before editing.

Problem solved: Found existing api-gateway changes and avoided overwriting them.

```bash
find . -maxdepth 3 -type d | sort | sed 's#^./##' | head -200
```

Why it was run: To understand repository structure.

Problem solved: Confirmed the multi-service layout under `services/`.

### Code Inspection

```bash
sed -n '1,240p' services/api-gateway/pom.xml
```

Why it was run: To inspect gateway dependencies and build config.

Problem solved: Confirmed Spring Cloud Gateway dependencies and added/validated Spring Security OAuth2 resource server support.

```bash
sed -n '1,260p' services/api-gateway/src/main/java/com/nkeanyi/gateway/filter/TenantCorrelationGatewayFilter.java
```

Why it was run: To inspect tenant and correlation enforcement.

Problem solved: Identified partial behavior and updated it for strict tenant validation, correlation generation, public endpoint allowlist, and trusted source propagation.

```bash
sed -n '1,240p' services/api-gateway/src/main/java/com/nkeanyi/gateway/filter/CorrelationIdFilter.java
```

Why it was run: To check for duplicate correlation logic.

Problem solved: Found duplicate correlation behavior and removed the legacy filter.

```bash
sed -n '1,260p' services/api-gateway/src/main/java/com/nkeanyi/gateway/security/GatewaySecurityConfig.java
```

Why it was run: To inspect gateway security configuration.

Problem solved: Expanded JWT validation, scope extraction, route enforcement, and JSON 401/403 handling.

```bash
sed -n '1,260p' services/api-gateway/src/test/java/com/nkeanyi/gateway/filter/TenantCorrelationGatewayFilterTest.java
```

Why it was run: To inspect existing tests.

Problem solved: Found only a basic header-name test and expanded coverage.

```bash
rg -n "spring:|cloud:|gateway|routes|issuer|keycloak|8080|actuator|api/v1" services/api-gateway services -g '*.yml' -g '*.yaml' -g '*.properties'
```

Why it was run: To find gateway routes, actuator exposure, and auth configuration.

Problem solved: Confirmed routes for documents, knowledge, payments, and evidence.

```bash
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@PatchMapping|api/v1|payments|evidence|documents|knowledge" services/*/src/main/java -g '*.java'
```

Why it was run: To map downstream APIs to gateway scope policies.

Problem solved: Confirmed route-level scope requirements for read/write APIs.

### Build And Unit Tests

```bash
mvn -q test
```

Why it was run: To compile and run api-gateway tests.

Problem solved: The first run caught a test API mismatch using `URI` instead of string paths. After fixing, tests passed.

```bash
mvn -q package
```

Why it was run: To verify the api-gateway Maven build.

Problem solved: Confirmed the module builds successfully.

### Runtime Checks

```bash
docker ps --format '{{.Names}} {{.Status}} {{.Ports}}'
```

Why it was run: To check which supporting containers were running.

Problem solved: Confirmed local infrastructure was running but Keycloak was not.

```bash
lsof -tiTCP:8080 -sTCP:LISTEN
```

Why it was run: To check whether port `8080` was available.

Problem solved: Confirmed the gateway could be started on port `8080`.

```bash
docker compose up -d keycloak
```

Why it was run: To start the repo Keycloak container for real JWT runtime testing.

Problem solved: Identified an external registry blocker: pulling `quay.io/keycloak/keycloak:26.1.5` failed with `502 Bad Gateway`.

```bash
python3 -c 'import cryptography; print("cryptography-ok")'
```

Why it was run: To confirm Python could generate signed JWTs for a local runtime JWKS issuer.

Problem solved: Confirmed `cryptography` was available.

```bash
python3 <temporary-local-jwks-issuer-script>
```

Why it was run: To start a temporary local issuer and JWKS endpoint when Keycloak could not be pulled.

Problem solved: Allowed end-to-end gateway JWT validation testing without weakening gateway behavior.

```bash
JWT_ISSUER_URI=http://127.0.0.1:19090/realms/payment-platform \
JWT_JWK_SET_URI=http://127.0.0.1:19090/certs \
mvn -q spring-boot:run
```

Why it was run: To start api-gateway on port `8080` with the temporary JWKS issuer.

Problem solved: Confirmed the gateway starts successfully on port `8080`.

```bash
python3 <temporary-downstream-echo-service-script>
```

Why it was run: To start a temporary downstream service on payment route port `8083`.

Problem solved: Verified downstream services receive `X-Tenant-Id`, `X-Correlation-Id`, and `X-Request-Source`.

### Runtime Curl Tests

```bash
curl http://127.0.0.1:8080/actuator/health
```

Why it was run: To verify public health endpoint access.

Problem solved: Returned `200`.

```bash
curl http://127.0.0.1:8080/actuator/info
```

Why it was run: To verify public info endpoint access.

Problem solved: Returned `200`.

```bash
curl -H "Authorization: Bearer <valid-read-token>" \
  http://127.0.0.1:8080/api/v1/payments
```

Why it was run: To test a protected request without `X-Tenant-Id`.

Problem solved: Returned `400 Bad Request` JSON response.

```bash
curl -H "X-Tenant-Id: tenant-001" \
  http://127.0.0.1:8080/api/v1/payments
```

Why it was run: To test a protected request without bearer token.

Problem solved: Returned `401 Unauthorized` JSON response.

```bash
curl -H "X-Tenant-Id: tenant-001" \
  -H "Authorization: Bearer <valid-write-token>" \
  http://127.0.0.1:8080/api/v1/payments
```

Why it was run: To test valid token with wrong scope against a payment read API.

Problem solved: Returned `403 Forbidden` JSON response.

```bash
curl -H "X-Tenant-Id: tenant-001" \
  -H "X-Correlation-Id: corr-runtime-001" \
  -H "X-Request-Source: untrusted-client" \
  -H "Authorization: Bearer <valid-read-token>" \
  http://127.0.0.1:8080/api/v1/payments
```

Why it was run: To test a valid tenant, token, and scope.

Problem solved: Returned `200`; downstream received normalized headers:

```json
{
  "tenant": "tenant-001",
  "correlation": "corr-runtime-001",
  "source": "api-gateway"
}
```

### Git Commit

```bash
git add services/api-gateway/pom.xml \
  services/api-gateway/src/main/java/com/nkeanyi/gateway/filter/CorrelationIdFilter.java \
  services/api-gateway/src/main/java/com/nkeanyi/gateway/filter/TenantCorrelationGatewayFilter.java \
  services/api-gateway/src/main/java/com/nkeanyi/gateway/security/GatewaySecurityConfig.java \
  services/api-gateway/src/main/resources/application.yml \
  services/api-gateway/src/test/java/com/nkeanyi/gateway/filter/TenantCorrelationGatewayFilterTest.java \
  services/api-gateway/src/test/java/com/nkeanyi/gateway/security/GatewaySecurityConfigTest.java
```

Why it was run: To stage only MVP 5 api-gateway files.

Problem solved: Prepared the completed implementation for commit.

```bash
git commit -m "Implement MVP 5 gateway security enforcement"
```

Why it was run: To create the requested git commit.

Problem solved: Created commit `80d87ef`.

## Problems Encountered And Resolved

1. Duplicate correlation behavior existed.

Resolution: Removed `CorrelationIdFilter.java` and consolidated behavior in `TenantCorrelationGatewayFilter`.

2. Public path matching was too broad.

Resolution: Restricted public access to `/actuator/health` and `/actuator/info`.

3. Initial tests were too shallow.

Resolution: Added tests for tenant rejection, invalid tenant format, correlation generation, header propagation, public health access, JWT scope extraction, `scp` extraction, and Keycloak role extraction.

4. Keycloak container could not be pulled.

Resolution: Used a temporary local JWKS issuer for runtime JWT validation tests.

5. The packaged jar was not executable.

Resolution: Used `mvn -q spring-boot:run` to start the gateway successfully on port `8080`.

## Sample Demo Test Data

Use this sample tenant and correlation data:

```text
X-Tenant-Id: tenant-demo-001
X-Correlation-Id: corr-demo-mvp5-001
X-Request-Source: external-client-demo
```

The gateway forwards this downstream as:

```text
X-Tenant-Id: tenant-demo-001
X-Correlation-Id: corr-demo-mvp5-001
X-Request-Source: api-gateway
```

### Payment Create Demo

Required scope:

```text
payments.write
```

```bash
curl -i -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <TOKEN_WITH_payments.write>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-mvp5-demo-001" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-mvp5-payment-001" \
  -d '{
    "tenantId": "tenant-demo-001",
    "customerId": "cust-demo-001",
    "amount": 125.50,
    "currency": "USD",
    "paymentMethod": "CARD",
    "description": "MVP 5 gateway payment demo"
  }'
```

### Payment Read Demo

Required scope:

```text
payments.read
```

```bash
curl -i http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <TOKEN_WITH_payments.read>" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-mvp5-payment-read-001"
```

### Evidence Read Demo

Required scope:

```text
evidence.read
```

```bash
curl -i http://localhost:8080/api/v1/evidence/correlation/corr-demo-mvp5-payment-001 \
  -H "Authorization: Bearer <TOKEN_WITH_evidence.read>" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-mvp5-evidence-001"
```

### Document Processing Demo

Required scope:

```text
documents.write
```

```bash
curl -i -X POST http://localhost:8080/api/v1/documents/analyze \
  -H "Authorization: Bearer <TOKEN_WITH_documents.write>" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-mvp5-documents-001" \
  -d '{
    "documentName": "invoice-demo-001.pdf",
    "documentType": "INVOICE"
  }'
```

### Knowledge Query Demo

Required scope:

```text
knowledge.read
```

```bash
curl -i -X POST http://localhost:8080/api/v1/knowledge/answer \
  -H "Authorization: Bearer <TOKEN_WITH_knowledge.read>" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-demo-001" \
  -H "X-Correlation-Id: corr-demo-mvp5-knowledge-001" \
  -d '{
    "query": "What evidence exists for payment approval?"
  }'
```

## Expected Negative Demo Results

Missing tenant:

```bash
curl -i http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <TOKEN_WITH_payments.read>"
```

Expected result:

```http
400 Bad Request
```

Missing token:

```bash
curl -i http://localhost:8080/api/v1/payments \
  -H "X-Tenant-Id: tenant-demo-001"
```

Expected result:

```http
401 Unauthorized
```

Wrong scope:

```bash
curl -i http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <TOKEN_WITH_payments.write>" \
  -H "X-Tenant-Id: tenant-demo-001"
```

Expected result:

```http
403 Forbidden
```

Public health:

```bash
curl -i http://localhost:8080/actuator/health
```

Expected result:

```http
200 OK
```
