# MVP14 Demo Dashboard UI Guide

MVP14 adds a lightweight browser dashboard for non-coders to inspect the regulated trust flow through the MVP13 observability endpoint.

## What It Shows

- Platform name: Nkeanyi-AI Trust Flow System
- Tenant ID and readiness status
- Total evidence and total correlations
- Evidence counts by type
- Recent trust flows and their status
- Source services involved in the recent trust flows
- Last event time

## Prerequisites

Start the NAITFS platform, then obtain a JWT with `evidence.read` scope.

```bash
./scripts/start-naitfs-platform.sh
./scripts/get-token.sh
export TOKEN="$(cat .token)"
```

Paste the raw JWT value from `.token` into the dashboard token field.

## Run The Dashboard

From the repo root:

```bash
python3 -m http.server 4173
```

Open:

```text
http://127.0.0.1:4173/dashboard/
```

Defaults:

- Gateway URL: `http://localhost:8080`
- Tenant ID: `tenant-mvp13-demo`

Paste the bearer token into the dashboard and select **Refresh Dashboard**. The dashboard calls:

```text
GET http://localhost:8080/api/v1/evidence/demo-dashboard?tenantId={tenantId}
```

through the API Gateway.

## Validate MVP14

With the platform running and `TOKEN` set:

```bash
TOKEN="$TOKEN" ./scripts/validate-mvp14-dashboard.sh
```

Optional environment variables:

- `GATEWAY_URL`, default `http://localhost:8080`
- `TENANT_ID`, default `tenant-mvp13-demo`
- `DASHBOARD_URL`, default `http://127.0.0.1:4173/dashboard/`

## Compatibility

MVP14 only adds the static dashboard, local dashboard CORS support, and a validation script. It does not change the MVP11 shell demo, the MVP12 Newman runner, or the MVP13 endpoint response contract.
