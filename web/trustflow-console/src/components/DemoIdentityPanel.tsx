import { useEffect, useState } from "react";
import {
  clearDemoIdentity,
  generateDemoIdentity,
  getDemoIdentity,
  saveDemoIdentity,
  type DemoIdentity,
} from "../utils/demoStorage";

type DemoIdentityPanelProps = {
  onIdentityChange: (identity: DemoIdentity) => void;
  title?: string;
};

export function DemoIdentityPanel({ onIdentityChange, title = "Demo Identity" }: DemoIdentityPanelProps) {
  const [identity, setIdentity] = useState(getDemoIdentity);

  useEffect(() => {
    onIdentityChange(identity);
  }, [identity, onIdentityChange]);

  function updateIdentity(field: keyof DemoIdentity, value: string) {
    setIdentity((current) => {
      const nextIdentity = {
        ...current,
        [field]: value,
      };
      saveDemoIdentity(nextIdentity);
      return nextIdentity;
    });
  }

  function generateNewIdentity() {
    const nextIdentity = generateDemoIdentity();
    saveDemoIdentity(nextIdentity);
    setIdentity(nextIdentity);
  }

  function reuseExistingIdentity() {
    setIdentity(getDemoIdentity());
  }

  function clearIdentity() {
    clearDemoIdentity();
    const nextIdentity = getDemoIdentity();
    saveDemoIdentity(nextIdentity);
    setIdentity(nextIdentity);
  }

  return (
    <section className="panel demo-identity-panel">
      <div className="panel-header">
        <div>
          <h3>{title}</h3>
          <p>These values persist locally and are shared by document, knowledge, payment, and evidence calls.</p>
        </div>
      </div>
      <div className="form-grid">
        <label>
          Tenant ID
          <input value={identity.tenantId} onChange={(event) => updateIdentity("tenantId", event.target.value)} />
        </label>
        <label>
          Correlation ID
          <input
            value={identity.correlationId}
            onChange={(event) => updateIdentity("correlationId", event.target.value)}
          />
        </label>
        <label>
          Idempotency Key
          <input
            value={identity.idempotencyKey}
            onChange={(event) => updateIdentity("idempotencyKey", event.target.value)}
          />
        </label>
      </div>
      <div className="button-row">
        <button type="button" onClick={generateNewIdentity}>
          Generate New Demo Identity
        </button>
        <button type="button" className="secondary-button" onClick={reuseExistingIdentity}>
          Reuse Existing Demo Identity
        </button>
        <button type="button" className="secondary-button" onClick={clearIdentity}>
          Clear Demo Identity
        </button>
      </div>
    </section>
  );
}
