import { PageHeader, ProductCard } from "../components/ProductUI";

const executiveScript = [
  "Open with the problem: regulated teams cannot rely on opaque AI or payment workflows when they cannot prove who initiated an action, which tenant it belonged to, what services handled it, and what evidence was recorded.",
  "Show the dashboard: explain that NAITFS packages identity, tenant isolation, correlation IDs, idempotency, and evidence capture into one trust-flow console.",
  "Run the Trust Flow Demo: process a document, generate grounded knowledge, execute a payment workflow, and capture evidence using one tenant and correlation ID.",
  "Close on value: NAITFS reduces production risk by making workflow execution traceable, repeatable, and audit-ready.",
];

const technicalScript = [
  "Start with Auth Diagnostics to show Keycloak realm, client, token availability, and development fallback boundaries.",
  "Show Demo Identity: tenant ID, correlation ID, and idempotency key are propagated across services.",
  "Run Documents and Knowledge to show controlled extraction and grounded answers with citations.",
  "Run Payments twice with the same idempotency key to explain duplicate execution prevention.",
  "Open Evidence to show the ledger and timeline that connect service activity into a reviewable audit chain.",
  "Use Health to confirm the gateway and platform readiness posture.",
];

const buyerMessages = [
  "Reduce production failures",
  "Prove workflow execution",
  "Prevent duplicate payment execution",
  "Trace cross-service activity",
  "Preserve compliance evidence",
];

const objections = [
  [
    "How is this different from logging?",
    "Logs are operational exhaust. NAITFS packages identity, tenant, correlation, idempotency, and evidence into a business-reviewable workflow record.",
  ],
  [
    "How is this different from a normal AI chatbot?",
    "The product is not just answer generation. It controls the workflow around AI outputs: grounding, traceability, evidence, and business execution.",
  ],
  [
    "Why does idempotency matter?",
    "Retries happen in production. Idempotency reduces the chance that a payment or business action is executed twice.",
  ],
  [
    "Why does correlation ID matter?",
    "A correlation ID connects activity across gateway, documents, knowledge, payments, and evidence into one reviewable chain.",
  ],
  [
    "Why does tenant isolation matter?",
    "Enterprise buyers need assurance that customer or business-unit workflow context is separated and consistently propagated.",
  ],
];

export function DemoScriptPage() {
  return (
    <section className="page-stack">
      <PageHeader
        title="Presenter Demo Script"
        subtitle="Commercial talk tracks for executives, buyers, compliance leaders, and technical reviewers."
      />

      <ProductCard title="3-Minute Executive Demo Script" subtitle="Buyer-first narrative for a customer discovery call.">
        <ol className="demo-flow-list">
          {executiveScript.map((step) => (
            <li key={step}>{step}</li>
          ))}
        </ol>
      </ProductCard>

      <ProductCard title="7-Minute Technical Demo Script" subtitle="Engineering credibility without making the UI developer-first.">
        <ol className="demo-flow-list">
          {technicalScript.map((step) => (
            <li key={step}>{step}</li>
          ))}
        </ol>
      </ProductCard>

      <ProductCard title="Key Buyer Messages" subtitle="Commercial points to repeat during the walkthrough.">
        <div className="risk-grid">
          {buyerMessages.map((message) => (
            <span key={message}>{message}</span>
          ))}
        </div>
      </ProductCard>

      <ProductCard title="Objection Handling" subtitle="Short answers for common enterprise buyer questions.">
        <div className="key-terms-grid objections-grid">
          {objections.map(([question, answer]) => (
            <article key={question}>
              <strong>{question}</strong>
              <p>{answer}</p>
            </article>
          ))}
        </div>
      </ProductCard>
    </section>
  );
}
