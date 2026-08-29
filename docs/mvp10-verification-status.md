# MVP 10 Verification Status

Date: 2026-05-24

## Local Unit Verification

Default local verification does not require Docker:

```bash
mvn test
```

This command runs unit and local context tests only. Payment integration tests that require Testcontainers are excluded from Surefire by default.

Latest result: passed.

## MVP 10 Targeted Failure-Path Tests

The targeted MVP 10 failure-path hardening tests passed:

```bash
mvn -pl services/api-gateway,services/compliance-evidence-service,services/document-intelligence-service,services/knowledge-service test
mvn -pl services/payment-orchestration-service -Dtest=OutboxPublisherFailureTest test
```

Covered paths include document extraction failure, knowledge answer failure/fallback, invalid JWT rejection, document and knowledge idempotency, compliance failure evidence, and Kafka outbox publish failure handling.

## Docker/Testcontainers Integration Tests

Payment integration tests are tagged with `@Tag("integration")` and run under Failsafe. They require a working Docker environment because they start PostgreSQL and Kafka Testcontainers.

Run them with:

```bash
mvn verify -Pintegration-tests
```

Latest local check: `mvn -pl services/payment-orchestration-service verify -Pintegration-tests` reached the Failsafe phase and attempted the `*IntegrationTest` suite, then stopped because Testcontainers could not find a valid Docker environment.

Use the same command for full integration verification once Docker is available. Required prerequisites:

- Docker daemon running and reachable by Testcontainers.
- Permission for the current user/session to access Docker.
- Network access to pull the configured container images if they are not already present locally.

The `full-verification` profile also enables integration-test execution:

```bash
mvn verify -Pfull-verification
```

## Profiles

- `unit-tests`: keeps Docker/Testcontainers integration tests skipped.
- `local-no-docker`: same local-only behavior as the default `mvn test` path.
- `integration-tests`: enables Failsafe execution of `@Tag("integration")` / `*IntegrationTest` tests.
- `full-verification`: enables the same integration-test execution for complete verification.
