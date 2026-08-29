# MVP11 Demo Guide

## What MVP11 Proves

MVP11 makes NAITFS demo-ready for a regulated enterprise workflow. It proves that a single business flow can preserve tenant context, correlation context, JWT security, idempotency, document extraction, grounded knowledge answers, payment orchestration, and immutable compliance evidence.

The demo ends with a trust certificate showing whether the workflow is `VERIFIED`.

## Start the Platform

```bash
./scripts/start-naitfs-platform.sh
```

The script starts Docker infrastructure, repairs the Keycloak demo realm if needed, starts all five Spring Boot services, waits for `/actuator/health`, and prints `READY FOR DEMO`.

## Run the Demo

```bash
./scripts/demo-regulated-trust-flow.sh
```

Expected result:

```text
PASS: api-gateway is UP
PASS: document-intelligence-service is UP
PASS: knowledge-service is UP
PASS: payment-orchestration-service is UP
PASS: compliance-evidence-service is UP
PASS: Document extraction verified
PASS: Knowledge answer generation verified
PASS: Payment received
PASS: Duplicate idempotency behavior verified
PASS: Evidence summary auditStatus COMPLETE
PASS: Trust certificate VERIFIED
NAITFS Regulated Trust Flow Certificate
```

## Reset Demo Data

```bash
./scripts/reset-demo-data.sh
```

This removes MVP11 demo records from payment and evidence tables without deleting Docker volumes or resetting infrastructure. It is safe to rerun.

## Explaining NAITFS To Non-Coders

NAITFS is a regulated trust-flow platform. It connects business actions to evidence automatically.

In the demo, a user uploads a policy document, asks a question about it, creates a payment, and then asks the system to prove what happened. NAITFS keeps the tenant ID, correlation ID, security controls, payment events, document events, and compliance evidence tied together so an auditor can follow the full story.

The certificate is the simple business output: it says whether the workflow has enough evidence to be trusted.

## Known Limitations

- Demo data uses local Docker infrastructure and local Keycloak credentials.
- The demo is optimized for a happy-path trust flow plus security/idempotency negative checks.
- Evidence rules are MVP-level checks based on required event types.
- The certificate is an API response, not a digitally signed PDF.

## Next MVP Recommendation

MVP12 should add policy-driven control definitions, signed certificate export, evidence retention policies, and dashboard views for non-technical compliance users.
