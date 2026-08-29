# MVP15 One-Command Demo Guide

MVP15 packages the local NAITFS demo into one reviewer-facing command. It starts the platform, checks service health, resets demo data, runs the regulated trust flow, optionally runs the MVP12 Postman/Newman validation, verifies MVP13 endpoint shapes, validates the MVP14 dashboard files, and writes a demo summary.

## What MVP15 Validates

- Docker infrastructure and Spring Boot services are running.
- API Gateway, payment orchestration, compliance evidence, knowledge, and document intelligence services report `UP`.
- MVP11 regulated trust flow still completes.
- MVP12 external API validation runs when `newman` is installed.
- MVP13 correlation summary and demo dashboard endpoints return compatible JSON through the API Gateway.
- MVP14 dashboard files exist and call the MVP13 demo dashboard endpoint.

## Prerequisites

Install:

- Docker
- Java 21
- Maven
- curl
- jq
- Newman, optional for MVP12 validation

The script expects the local service ports used by NAITFS to be available, including `8080`, `8081`, `8082`, `8084`, `8088`, `8089`, `5432`, `9092`, `6333`, `9000`, and `16686`.

## Run The One-Command Demo

From the repo root:

```bash
./scripts/run-naitfs-demo.sh
```

Optional overrides:

```bash
GATEWAY_URL=http://localhost:8080 \
DASHBOARD_URL=http://127.0.0.1:4173/dashboard/ \
./scripts/run-naitfs-demo.sh
```

## Expected Output

The script prints section headers for tool checks, platform startup, service health, MVP11, MVP12, MVP13, and MVP14 validation. It writes:

```text
target/naitfs-demo-summary.txt
```

The final output includes:

- Gateway URL
- Dashboard path and suggested dashboard URL
- Tenant ID used
- Correlation ID
- Evidence count
- Postman validation status
- Dashboard validation status

Successful runs end with:

```text
READY FOR ONE-COMMAND NAITFS DEMO
```

## Open The Dashboard

In a second terminal from the repo root:

```bash
python3 -m http.server 4173
```

Open:

```text
http://127.0.0.1:4173/dashboard/
```

Paste a token from `.token` or run:

```bash
./scripts/get-token.sh
cat .token
```

Use the tenant ID from `target/naitfs-demo-summary.txt`.

## Troubleshooting

Port conflicts: stop the process using the conflicting port or change the relevant service configuration. Common ports are `8080`, `8081`, `8082`, `8084`, `8088`, and `8089`.

Keycloak token failure: run `./scripts/repair-keycloak-demo.sh`, then `./scripts/get-token.sh`. Confirm Keycloak is reachable at `http://localhost:8081/realms/payment-platform`.

Service health failure: inspect service logs under `logs/` and rerun `./scripts/start-naitfs-platform.sh`. The MVP15 script waits for `/actuator/health` to return `{"status":"UP"}`.

Missing Newman: MVP12 Postman validation is skipped when `newman` is unavailable. Install it with `npm install -g newman` and rerun the script.

Empty dashboard data: confirm the tenant ID in the dashboard matches `Tenant ID Used` in `target/naitfs-demo-summary.txt`, then refresh the dashboard after the MVP11 flow has completed.
