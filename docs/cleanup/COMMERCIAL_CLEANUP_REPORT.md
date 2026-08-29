# NAITFS Commercial Cleanup Report

## Scope

This report reviews the uploaded `nkeanyi-ai-trust-flow-system` project as a commercial software asset, not as a class/MVP artifact.

## Immediate cleanup performed in this sanitized package

Removed from the cleaned package:

- `.token`
- `scripts/.token`
- `web/trustflow-console/.env.local`
- `.pids/`
- `web/trustflow-console/dist/`
- `jobinfo1.pdf`

Updated:

- `.gitignore` now ignores frontend build output, local env files, token files, PID files, IDE folders, logs, coverage, and temp files.

## Critical findings

### 1. Committed JWT files

The original zip contained `.token` and `scripts/.token`. These are local demo JWTs, but they must never be committed or distributed. Delete them from the repo history before public GitHub release if they were ever committed.

Recommended command:

```bash
rm -f .token scripts/.token
git rm --cached .token scripts/.token 2>/dev/null || true
```

### 2. Local frontend environment file

The original zip contained `web/trustflow-console/.env.local`. Keep only `.env.example` in Git. Local `.env.local` belongs on the developer machine only.

### 3. Public release includes non-product artifact

The root contained `jobinfo1.pdf`, which appears unrelated to NAITFS product code. Remove from product repo.

### 4. Demo defaults are acceptable only for local use

`.env.example` and Docker Compose use demo credentials such as `admin`, `test-password`, `naitfs_password`, and `minioadmin`. That is acceptable for local demo examples only, but the product must clearly state these are not production credentials.

### 5. Payment service security defaults still need hardening

`services/payment-orchestration-service/src/main/resources/application.properties` sets:

```properties
app.security.enabled=${APP_SECURITY_ENABLED:false}
```

For production, security should fail closed. Recommended change:

```properties
app.security.enabled=${APP_SECURITY_ENABLED:true}
```

Keep the unauthenticated mode only in `application-local.properties` or a `local` Spring profile.

### 6. Payment create request validation is not enforced at controller boundary

`PaymentController#createPayment` accepts `@RequestBody CreatePaymentRequest request` without `@Valid`, and `CreatePaymentRequest` has no Bean Validation annotations. There is a `PaymentValidationService`, but it is not clearly invoked in `PaymentServiceImpl#createPayment`.

Recommended commercial fix:

- Add `spring-boot-starter-validation` if missing.
- Add annotations such as `@NotBlank`, `@NotNull`, `@Positive`, `@Size`, `@Pattern`.
- Change controller to `@Valid @RequestBody CreatePaymentRequest request`.
- Return structured 400 responses for validation failures.

### 7. Idempotency replay does not verify request equivalence

`PaymentServiceImpl#createPayment` returns the original payment for any reused idempotency key. A production-grade payment workflow should reject reuse of the same idempotency key with different request details.

Recommended commercial fix:

- Store a request fingerprint/hash on payment creation.
- On replay, recompute fingerprint and compare.
- Return `409 Conflict` when the same idempotency key is reused with a different request body.

### 8. Outbox publisher marks events as published before Kafka send confirmation

`OutboxPublisher#sendRecord` calls `kafkaTemplate.send(record)` but does not wait for broker acknowledgment before setting status to `PUBLISHED`.

Recommended commercial fix:

- Use `kafkaTemplate.send(record).get(...)`, or CompletableFuture callback with status update after success.
- Track retry count and last error.
- Keep failed events retryable instead of permanently marking them `FAILED` after one exception.

### 9. Tenant isolation should be enforced in persistence queries, not only headers

The gateway validates and propagates `X-Tenant-Id`, but commercial tenant isolation must be enforced at data-access level.

Recommended commercial fix:

- Add `tenant_id` to every tenant-owned record.
- Scope every lookup by tenant.
- Never expose `GET /api/v1/payments` globally across tenants.
- Prefer `/api/v1/payments/{paymentId}` with tenant-scoped lookup.

### 10. README is MVP-history heavy

The current README reads like an internal development log. For buyers, investors, or public GitHub users, rewrite it around:

- product purpose
- architecture
- quick start
- demo flow
- security model
- commercial readiness status
- roadmap

Move MVP history into `docs/archive/`.

## Recommended target repo structure

```text
nkeanyi-ai-trust-flow-system/
  README.md
  docs/
    commercial/
    architecture/
    demo/
    operations/
    archive/
  platform/
    docker/
    keycloak/
    prometheus/
    grafana/
  services/
    api-gateway/
    document-intelligence-service/
    knowledge-service/
    payment-orchestration-service/
    compliance-evidence-service/
  web/
    trustflow-console/
  scripts/
    demo/
    dev/
    ops/
  postman/
```

## 30-day commercial-readiness priorities

1. Sanitize repository.
2. Replace README with commercial README.
3. Create one stable `./scripts/demo/start-commercial-demo.sh` entry point.
4. Make security default `true` outside local profile.
5. Enforce validation and idempotency equivalence.
6. Add evidence export artifact: JSON first, PDF later.
7. Remove or hide demo-only billing from production wording.
8. Deploy a stable cloud demo environment.
9. Prepare sales deck, demo script, pilot agreement, and landing page.
10. Start founder-led outbound sales for paid pilots.

## Verification limitation

Static inspection was completed. Maven tests were not executed in this sandbox because `mvn` was not installed in the execution environment. Script syntax was checked with `bash -n` and no shell syntax failures were found.
