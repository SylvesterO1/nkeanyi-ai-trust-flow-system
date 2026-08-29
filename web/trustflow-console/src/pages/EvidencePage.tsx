import { useState } from "react";
import { getEvidenceByCorrelationId, getEvidenceByPaymentId } from "../api/evidenceApi";
import { ApiErrorPanel } from "../components/ApiErrorPanel";
import { AuthNotice } from "../components/AuthNotice";
import { DemoAuthPanel } from "../components/DemoAuthPanel";
import { DemoIdentityPanel } from "../components/DemoIdentityPanel";
import { EvidenceTable } from "../components/EvidenceTable";
import { EvidenceTimeline, PageHeader, ProductCard, TechnicalDetails } from "../components/ProductUI";
import { WhatThisProves } from "../components/WhatThisProves";
import { useDemoIdentity, useDemoToken } from "../utils/demoStorage";
import type { EvidenceSearchResponse } from "../types/api";
import { extractEvidenceRecords } from "./evidenceUtils";

export function EvidencePage() {
  const demoIdentity = useDemoIdentity();
  const { token } = useDemoToken();
  const [paymentId, setPaymentId] = useState("");
  const [response, setResponse] = useState<EvidenceSearchResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  const records = extractEvidenceRecords(response);
  const requestOptions = {
    tenantId: demoIdentity.tenantId,
    correlationId: demoIdentity.correlationId,
    authorizationToken: token || undefined,
  };

  async function searchByCorrelationId() {
    setError(null);
    setLoading(true);
    try {
      setResponse(await getEvidenceByCorrelationId(demoIdentity.correlationId, requestOptions));
    } catch (err) {
      setResponse(null);
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  async function searchByPaymentId() {
    setError(null);
    setLoading(true);
    try {
      setResponse(await getEvidenceByPaymentId(paymentId, requestOptions));
    } catch (err) {
      setResponse(null);
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-stack">
      <PageHeader
        title="Compliance Evidence Ledger"
        subtitle="Search audit trail and governance evidence by tenant, correlation ID, or payment workflow."
      />

      <DemoAuthPanel />
      <AuthNotice />
      <DemoIdentityPanel onIdentityChange={demoIdentity.setDemoIdentity} />

      <WhatThisProves
        title="What This Proves"
        points={[
          "Compliance evidence records prove what happened",
          "Success and failure events are auditable",
          "Correlation ID connects the full workflow",
        ]}
      />

      <ProductCard title="Search Evidence" subtitle="Find evidence records tied to the current tenant and workflow context.">
        <div className="form-grid">
          <label>
            Tenant ID
            <input value={demoIdentity.tenantId} readOnly />
          </label>
          <label>
            Correlation ID
            <input value={demoIdentity.correlationId} readOnly />
          </label>
          <label>
            Payment ID
            <input value={paymentId} onChange={(event) => setPaymentId(event.target.value)} />
          </label>
        </div>
        <div className="button-row">
          <button
            type="button"
            onClick={searchByCorrelationId}
            disabled={!demoIdentity.correlationId || loading}
          >
            Search by Correlation ID
          </button>
          <button type="button" onClick={searchByPaymentId} disabled={!paymentId || loading}>
            Search by Payment ID
          </button>
        </div>
        <ApiErrorPanel error={error} />
      </ProductCard>

      <ProductCard title="Evidence Timeline" subtitle="Timeline cards show the business event before raw ledger rows.">
        <EvidenceTimeline records={records} />
      </ProductCard>

      <ProductCard title="Evidence Table" subtitle="Structured ledger view for compliance and engineering review.">
        <EvidenceTable records={records} />
      </ProductCard>

      <ProductCard title="Technical Details" subtitle="Raw evidence API response remains available but secondary.">
        <TechnicalDetails data={response} />
      </ProductCard>
    </section>
  );
}
