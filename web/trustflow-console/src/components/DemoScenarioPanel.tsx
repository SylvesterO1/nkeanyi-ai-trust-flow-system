export function DemoScenarioPanel() {
  return (
    <section className="panel demo-scenario-panel">
      <div>
        <span>Scenario</span>
        <p>
          A regulated enterprise receives a business document, extracts key information, asks a grounded AI
          question, triggers a payment workflow, and stores compliance evidence for audit review.
        </p>
      </div>
      <div>
        <span>Business problem</span>
        <p>
          Production failures happen when systems cannot prove what happened, why it happened, who triggered it,
          and whether duplicate actions were prevented.
        </p>
      </div>
      <div>
        <span>NAITFS solution</span>
        <p>
          Use tenant ID, correlation ID, idempotency key, and compliance evidence to make workflows traceable,
          repeatable, and auditable.
        </p>
      </div>
    </section>
  );
}
