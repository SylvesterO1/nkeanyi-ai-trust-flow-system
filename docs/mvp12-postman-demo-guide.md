# MVP12 Postman Demo Guide

MVP12 packages the external API validation flow as a Postman/Newman demo for the local NAITFS platform.

## Prerequisites

- Local platform is running through the API Gateway on `http://localhost:8080`.
- Keycloak is available on `http://localhost:8081`.
- Newman is installed: `npm install -g newman`.
- MVP11 scripts remain unchanged; this demo only adds Postman assets and a new runner.

## Files

- `postman/NAITFS-MVP12-Regulated-Trust-Flow.postman_collection.json`
- `postman/NAITFS-MVP12-local.postman_environment.json`
- `scripts/run-mvp12-postman-tests.sh`
- `target/mvp12-postman-results.json` after a Newman run

## Run With Newman

```bash
./scripts/run-mvp12-postman-tests.sh
```

The runner checks that Newman is installed, verifies gateway health, creates the document upload fixture, runs the collection, and exports JSON results to:

```text
target/mvp12-postman-results.json
```

Successful output ends with:

```text
READY FOR EXTERNAL API DEMO
```

## Run In Postman

1. Import `postman/NAITFS-MVP12-Regulated-Trust-Flow.postman_collection.json`.
2. Import `postman/NAITFS-MVP12-local.postman_environment.json`.
3. Select the `NAITFS MVP12 Local` environment.
4. Ensure `target/mvp12-control.txt` exists. The Newman runner creates it automatically; for manual Postman use, create a small text file with the same path or update the file body path in the document request.
5. Run the full collection in order.

## Validated Flow

The collection validates gateway health, Keycloak token retrieval, valid JWT access, missing and invalid JWT rejection, document processing, grounded answer generation, payment creation, payment idempotency replay, compliance evidence lookup by `correlationId`, tenant isolation, and missing required header rejection.

The collection generates a fresh `correlationId` and `idempotencyKey` at the start of each full run and stores runtime outputs such as `accessToken`, `documentId`, `paymentId`, and `evidenceCount` in the Postman environment.
