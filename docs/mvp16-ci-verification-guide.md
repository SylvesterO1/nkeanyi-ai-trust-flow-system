# MVP16 CI Verification Guide

MVP16 adds GitHub Actions verification for pull requests and pushes to `main`. The workflow is intentionally stable: it validates build output, unit tests, scripts, JSON artifacts, dashboard assets, documentation, and Docker Compose syntax without starting the full NAITFS platform.

## What The Workflow Validates

- Java 21 setup and Maven dependency caching.
- Full Maven test suite with `mvn test`.
- Bash syntax for the MVP11-MVP15 demo scripts.
- MVP12 Postman collection and environment JSON with `jq`.
- Required MVP12-MVP15 documentation files exist.
- MVP14 dashboard entry point exists at `dashboard/index.html`.
- Docker Compose configuration parses with `docker compose config`.
- Maven test reports are uploaded as artifacts when available.

## Why Live Services Are Not Required Yet

The MVP16 workflow is meant to run reliably on every pull request. Starting Kafka, Postgres, Keycloak, Spring services, and the full regulated trust flow would make PR feedback slower and more sensitive to transient infrastructure timing. Full service orchestration remains covered by the local MVP15 demo script.

## Run Equivalent Checks Locally

From the repo root:

```bash
mvn test
```

```bash
bash -n scripts/start-naitfs-platform.sh
bash -n scripts/reset-demo-data.sh
bash -n scripts/demo-regulated-trust-flow.sh
bash -n scripts/run-mvp12-postman-tests.sh
bash -n scripts/validate-mvp14-dashboard.sh
bash -n scripts/run-naitfs-demo.sh
```

```bash
jq empty postman/NAITFS-MVP12-Regulated-Trust-Flow.postman_collection.json
jq empty postman/NAITFS-MVP12-local.postman_environment.json
```

```bash
test -f docs/mvp12-postman-demo-guide.md
test -f docs/mvp13-observability-dashboard-guide.md
test -f docs/mvp14-demo-dashboard-ui-guide.md
test -f docs/mvp15-one-command-demo-guide.md
test -f dashboard/index.html
```

```bash
docker compose -f platform/docker/docker-compose.naitfs.yml config >/tmp/naitfs-compose-config.yml
```

## Interpreting CI Failures

Maven failures usually mean a Java compile or unit test regression. Open the uploaded Surefire report artifact or inspect the failing module logs.

Bash syntax failures usually mean a script has invalid shell syntax, often from an unmatched quote, missing `fi`, or invalid conditional expression.

Postman JSON failures mean the collection or environment file is no longer valid JSON.

Documentation or dashboard checks mean a required MVP demo artifact was renamed, deleted, or not added to the pull request.

Docker Compose failures mean the compose file no longer parses or has invalid service configuration.

## Future Full Integration CI

A later CI milestone can add an opt-in integration workflow that starts Docker infrastructure, boots all Spring services, runs MVP15 end-to-end, and publishes the generated `target/naitfs-demo-summary.txt`. That workflow should run on a schedule or manual dispatch first, then graduate to protected branches after runtime and flake rate are acceptable.
