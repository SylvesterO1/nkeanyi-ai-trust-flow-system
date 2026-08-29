re# nkeanyi-ai-trust-flow-system

## MVP17 TrustFlow Console

MVP17 introduces the Nkeanyi TrustFlow Console, a React/TypeScript/Vite web interface for demonstrating NAITFS workflow visibility across payments, compliance evidence, and system health.

During local development, the API Gateway allows CORS requests from `http://localhost:5174` so the web console can call gateway endpoints such as `/actuator/health` and `/api/v1/payments` from the browser.

## MVP17.4 Document Records Visibility

MVP17.4 adds persisted document processing records and web console visibility for processed document history. The document intelligence service now stores records in `document_records` with tenant, correlation, document ID, filename, content type, extractor, confidence, status, extracted text, error message, and timestamps.

New backend endpoints:

```text
GET /api/v1/documents?tenantId=<tenant>&correlationId=<correlation>&limit=25
GET /api/v1/documents/{documentId}
GET /api/v1/documents/correlation/{correlationId}?tenantId=<tenant>&limit=25
```

List endpoints return `{ "records": [...], "count": 0 }`. `X-Tenant-Id` is honored as the tenant filter when `tenantId` is not supplied as a query parameter.
Verify with curl after the platform is running and a bearer token is available:

```bash
curl -sS -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: tenant-web-demo-001" \
  "http://localhost:8080/api/v1/documents?limit=5"

curl -sS -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/documents/{documentId}"

curl -sS -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: tenant-web-demo-001" \
  "http://localhost:8080/api/v1/documents/correlation/{correlationId}"
```

Verify from the web console:

```bash
cd /NAITFS/nkeanyi-ai-trust-flow-system
mvn test

cd /Users//NAITFS/nkeanyi-ai-trust-flow-system/web/trustflow-console
npm run build
npm run dev:5174
```

Open `http://localhost:5174/documents`, save a demo token if required, generate or reuse demo identity, upload and process a document, then use **Load Recent Records**, document ID search, correlation ID search, and **View Details**. The Trust Flow Demo page also includes **View Document Record** after the document step and includes `documentRecordStatus` in the final trust summary.

## MVP17.5 Billing and Usage Console

MVP17.5 adds a frontend-only `/billing` page for commercial SaaS and enterprise demos.

The Billing page shows demo SaaS pricing, current plan details, monthly usage breakdown, billable workflow events, projected monthly bill, usage by tenant, and invoice history. It is clearly labeled as Demo Billing Data.

Stripe and real payment collection are not implemented yet. Real billing should later be calculated from platform records, including `document_records`, knowledge answer events, payments, compliance evidence records, failed workflow records, and audit export records.

## MVP17.6 Executive Demo Polish

MVP17.6 polishes the TrustFlow Console for commercial demos to non-coders, business users, enterprise buyers, and technical reviewers.

Added:

- Executive dashboard messaging with clear value proposition and CTAs.
- What This Proves panels across core workflow pages.
- A reusable demo scenario panel for the Dashboard and Trust Flow Demo.
- `/demo-script` route with opening pitch, demo flow, key terms, and closing statement.
- Final MVP17 completion checklist on the Dashboard and Demo Script pages.

For non-coder presentations, start at `http://localhost:5174/dashboard`, explain the business problem, run the Trust Flow Demo, show evidence by correlation ID, then show Billing to explain how usage can become a traceable commercial model.

```bash
cd web/trustflow-console
npm run build
npm run dev:5174
```

Open:

- `http://localhost:5174/dashboard`
- `http://localhost:5174/demo-script`

## MVP18 Keycloak Login Integration

MVP18 adds browser-based Keycloak login while preserving the existing local demo-token fallback for development.

Required local auth configuration:

- Keycloak URL: `http://localhost:8081`
- Realm: `payment-platform`
- Client ID: `naitfs-client`
- API Gateway: `http://localhost:8080`

Frontend environment:

```bash
cd web/trustflow-console
cp .env.example .env.local
```

Expected values:

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=payment-platform
VITE_KEYCLOAK_CLIENT_ID=naitfs-client
```

Run the console:

```bash
cd web/trustflow-console
npm install
npm run build
npm run dev:5174
```

Open `http://localhost:5174`, use the header **Login** button to start the Keycloak redirect, and use **Logout** to end the browser session. The `/auth-debug` page shows authentication status, username, realm/client configuration, roles/scopes, and token expiry without printing the full token.

If Keycloak is not available during local testing, secured workflow pages still support the **Development Token Fallback** panel. Paste a bearer token there and the API client will use it when no Keycloak token is active.

Troubleshooting:

- `401` usually means no token is present, the token expired, or the browser login did not complete.
- `403` usually means the user is authenticated but the token lacks the required role or scope.
- Keycloak "We are sorry" during login usually means the `naitfs-client` redirect URI does not match the Vite dev server. Run `./scripts/repair-keycloak-demo.sh` and confirm Keycloak allows redirects for `http://localhost:5174/*` and `http://localhost:5173/*`, web origins for `http://localhost:5174`, `http://localhost:5173`, and `+`, and root/home URL `http://localhost:5174`.
- Check `/auth-debug` before testing Documents, Knowledge, Payments, Evidence, or Trust Flow Demo calls.

## MVP18.1 Enterprise UI Redesign

MVP18.1 upgrades the TrustFlow Console with a polished enterprise SaaS visual design while preserving existing frontend routes, Keycloak login/logout, demo token fallback, demo identity persistence, and API behavior.

Included:

- Redesigned app shell, grouped dark sidebar, route-aware header, and professional auth status.
- Executive dashboard with trust controls, KPI cards, Trust Flow Map, and operational risk controls.
- Polished workflow pages with clearer action areas, result summaries, empty states, and secondary technical details.
- Improved Trust Flow Demo page with scenario context, identity/auth panels, guided workflow steps, and a final audit-ready summary.
- SaaS-style Billing page with demo billing badge, plan summary, usage tables, tenant usage, invoice history, and commercialization note.
- No backend behavior changed.

## MVP18.2 Commercial Product UI Redesign

MVP18.2 productizes the console as **NkeanyiAI TrustFlow**, a regulated enterprise AI Trust Flow platform for buyer demos, executive walkthroughs, compliance reviews, and engineering leadership.

Included:

- Buyer-first dashboard with executive positioning, trust posture, commercial KPIs, Trust Flow Map, and risk-control cards.
- Main Trust Flow Demo redesigned as an audit-ready product workflow proving identity, tenant context, document processing, grounded knowledge, payment orchestration, idempotency, and compliance evidence.
- Workflow pages reframed as product controls: Document Intelligence Control, Grounded Knowledge Control, Payment Orchestration Control, and Compliance Evidence Ledger.
- Operations/admin pages polished for Platform Operations Health, Auth Diagnostics, Billing roadmap, and presenter demo scripts.
- Raw API responses, Keycloak login/logout, Development Token Fallback, Auth Debug, demo identity persistence, and all backend API calls remain available.
- No backend behavior changed.

Guide: [docs/mvp18.2-commercial-ui-redesign.md](docs/mvp18.2-commercial-ui-redesign.md)

## MVP19 Commercial Demo Readiness

MVP19 proves the full commercial demo path works end to end:

Keycloak login -> TrustFlow Console -> API Gateway -> secured backend services -> document intelligence -> grounded knowledge -> payment orchestration -> compliance evidence ledger -> executive summary.

Run the smoke script:

```bash
cd /Users/NAITFS/nkeanyi-ai-trust-flow-system
./scripts/smoke-test-mvp19-commercial-demo.sh
```

Run the UI demo:

```bash
cd /Users/NAITFS/nkeanyi-ai-trust-flow-system/web/trustflow-console
npm run dev:5174
```

Open `http://localhost:5174/auth-debug`, log in with the demo Keycloak user, then run `http://localhost:5174/trust-flow-demo`.

Expected commercial demo summary:

- Workflow status: complete
- Tenant ID, correlation ID, and idempotency key shown
- Document ID and payment ID shown
- Evidence count greater than zero
- Final decision and risk level shown
- Message: NAITFS verified authenticated TrustFlow execution, tenant propagation, correlation tracing, document intelligence, grounded knowledge, idempotent payment orchestration, and compliance evidence capture.

Guide: [docs/mvp19-commercial-demo-readiness.md](docs/mvp19-commercial-demo-readiness.md)

## MVP20 Enterprise Document and Knowledge Workflow

MVP20 adds the enterprise workflow controls for document intelligence and governed knowledge retrieval.

Included:

- Document lifecycle states from intake through review, approval, publish, quarantine, deprecation, and failure.
- Classification, sensitivity, DLP, prompt-injection, confidence, and review quality gates.
- Curator review tasks, review decisions, publish blocking, feedback-created review work, audit search, and operational metrics.
- Knowledge runtime policy checks for authorized document retrieval, roles, prompt injection, DLP-sensitive evidence, no-evidence behavior, retrieval events, feedback, and evaluation metrics.

Guide: [docs/mvp20-enterprise-document-knowledge-workflow.md](docs/mvp20-enterprise-document-knowledge-workflow.md)

## MVP13 Observability Dashboard

MVP13 adds dashboard-ready compliance evidence summaries and demo metrics for executive trust-flow visibility.

Guide: [docs/mvp13-observability-dashboard-guide.md](docs/mvp13-observability-dashboard-guide.md)

## MVP14 Demo Dashboard UI

MVP14 adds a lightweight browser dashboard for executive trust-flow demos.

Guide: [docs/mvp14-demo-dashboard-ui-guide.md](docs/mvp14-demo-dashboard-ui-guide.md)

## MVP15 One-Command Demo

MVP15 packages the local platform startup and demo validation into one reviewer-facing command.

Guide: [docs/mvp15-one-command-demo-guide.md](docs/mvp15-one-command-demo-guide.md)

## MVP16 CI Verification

MVP16 adds pull request and main-branch CI checks for build, tests, scripts, JSON artifacts, dashboard assets, and docs.

Guide: [docs/mvp16-ci-verification-guide.md](docs/mvp16-ci-verification-guide.md)

## Commercialization

NAITFS is positioned as a commercial regulated enterprise trust-flow platform for compliance-sensitive organizations.

- [Product positioning](docs/commercial/product-positioning.md)
- [Buyer problem](docs/commercial/buyer-problem.md)
- [Ideal customer profile](docs/commercial/ideal-customer-profile.md)
- [Pilot offer](docs/commercial/pilot-offer.md)
- [Commercial demo script](docs/commercial/demo-script.md)
- [Pricing hypothesis](docs/commercial/pricing-hypothesis.md)
- [Customer discovery questions](docs/commercial/customer-discovery-questions.md)
- [Commercialization roadmap](docs/commercial/commercialization-roadmap.md)
- [Security and trust overview](docs/commercial/security-and-trust-overview.md)
- [Integration checklist](docs/commercial/integration-checklist.md)

## MVP12 External API Demo

MVP12 adds a Postman/Newman validation package for the regulated trust flow through the API Gateway.

Run it locally after the platform is up:

```bash
./scripts/run-mvp12-postman-tests.sh
```

Demo guide: [docs/mvp12-postman-demo-guide.md](docs/mvp12-postman-demo-guide.md)
