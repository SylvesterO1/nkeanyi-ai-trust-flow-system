# MVP19 Commercial Demo Readiness

## Purpose

MVP19 proves the commercial NAITFS TrustFlow demo works end to end:

Keycloak login -> TrustFlow Console -> API Gateway -> secured backend services -> document intelligence -> grounded knowledge -> payment orchestration -> compliance evidence ledger -> executive summary.

## Prerequisites

- Docker is running.
- Java and Maven are available.
- Node dependencies are installed in `web/trustflow-console`.
- Keycloak demo realm/client/user are configured by `scripts/repair-keycloak-demo.sh`.
- Required local ports are available:
  - API Gateway: `8080`
  - Keycloak: `8081`
  - Payment Orchestration: `8082`
  - Compliance Evidence: `8084`
  - Knowledge Service: `8088`
  - Document Intelligence: `8089`
  - Frontend: `5174`

## Start Platform

```bash
cd /Users/a47487/NAITFS/nkeanyi-ai-trust-flow-system
./scripts/start-naitfs-platform.sh
```

If services are already running, verify health:

```bash
./scripts/check-health.sh
```

Expected services:

- `api-gateway`
- `document-intelligence-service`
- `knowledge-service`
- `payment-orchestration-service`
- `compliance-evidence-service`
- Keycloak realm `payment-platform`

## Start Frontend

```bash
cd /Users/a47487/NAITFS/nkeanyi-ai-trust-flow-system/web/trustflow-console
npm run dev:5174
```

Open:

```text
http://localhost:5174/auth-debug
```

## Login

Use the configured demo user:

- Username: `test-user`
- Password: `test-password`
- Realm: `payment-platform`
- Client: `naitfs-client`

Expected Auth Diagnostics result:

- Authenticated: `Yes`
- Token Present: `Yes`
- Realm: `payment-platform`
- Client ID: `naitfs-client`

## Run Trust Flow Demo

Open:

```text
http://localhost:5174/trust-flow-demo
```

Run the workflow:

1. Confirm identity and tenant context.
2. Process a document.
3. Generate grounded knowledge.
4. Execute payment workflow.
5. Replay payment with the same idempotency key.
6. Capture compliance evidence.

Expected final panel:

- Title: `Audit-Ready Workflow Complete`
- Workflow status: `Complete`
- Evidence count greater than zero
- Tenant ID shown
- Correlation ID shown
- Document ID shown
- Payment ID shown
- Risk level shown
- Final decision shown

## CLI Smoke Test

Run:

```bash
cd /Users/a47487/NAITFS/nkeanyi-ai-trust-flow-system
./scripts/smoke-test-mvp19-commercial-demo.sh
```

Expected final output:

```text
READY FOR COMMERCIAL DEMO:
NAITFS verified authenticated TrustFlow execution, tenant propagation, correlation tracing, document intelligence, grounded knowledge, idempotent payment orchestration, and compliance evidence capture.
```

## Troubleshooting

### Keycloak "We are sorry"

Usually caused by an invalid redirect URI or local HTTP realm setting. Run:

```bash
./scripts/repair-keycloak-demo.sh
```

Confirm `naitfs-client` allows:

- `http://localhost:5174/*`
- `http://localhost:5173/*`

### Invalid Redirect URI

Confirm frontend env:

```bash
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=payment-platform
VITE_KEYCLOAK_CLIENT_ID=naitfs-client
VITE_API_BASE_URL=http://localhost:8080
```

Then restart the frontend.

### 401 or 403

- `401`: token missing, invalid, or expired.
- `403`: token exists but lacks required scopes.

Use `/auth-debug` to confirm token, realm, client, roles, and scopes.

### Backend Service Down

Check:

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS http://localhost:8089/actuator/health
curl -sS http://localhost:8088/actuator/health
curl -sS http://localhost:8082/actuator/health
curl -sS http://localhost:8084/actuator/health
```

Restart with:

```bash
./scripts/start-naitfs-platform.sh
```

### Evidence Not Found

Evidence is asynchronous. Wait a few seconds after document, knowledge, or payment actions, then search again by correlation ID.

If still empty, check Kafka and compliance evidence service logs.

### Idempotency Duplicate Issue

Replay the payment with the exact same `Idempotency-Key` and request body. Expected behavior is HTTP `200` with the original payment ID, not unsafe duplicate execution.

### CORS Issue

Confirm the frontend origin is `http://localhost:5174` and API Gateway CORS allows that origin during local development.

### Expired Token

Use `/auth-debug` to check token expiry. Log out and log in again to refresh the session.
