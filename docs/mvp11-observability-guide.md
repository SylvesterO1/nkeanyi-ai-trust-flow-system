# MVP11 Observability Guide

## Jaeger

Open Jaeger:

```text
http://localhost:16686
```

Use it to inspect distributed request traces across the API Gateway and downstream services. Select a service, search recent traces, and compare spans for the same user workflow.

## Correlation IDs

Every demo request uses `X-Correlation-Id`.

That value connects:

- API Gateway request logs
- downstream service logs
- Kafka events
- compliance evidence records
- evidence summary and certificate responses

For a demo, copy the correlation ID printed by `demo-regulated-trust-flow.sh` and use it in logs, Jaeger, and the evidence endpoints.

## Logs, Events, And Evidence

NAITFS uses three related layers:

- Logs explain what each service did.
- Events communicate regulated business facts between services.
- Evidence stores those facts as tenant-scoped audit records.

This means observability is not only operational troubleshooting. It also becomes compliance proof.

## Enterprise Buyer Explanation

For regulated buyers, observability answers: "Can we prove what happened, when it happened, who it belonged to, and whether required controls ran?"

NAITFS keeps a shared correlation ID through document, knowledge, payment, and compliance workflows. That gives enterprise teams a traceable chain from business request to technical execution to audit evidence.
