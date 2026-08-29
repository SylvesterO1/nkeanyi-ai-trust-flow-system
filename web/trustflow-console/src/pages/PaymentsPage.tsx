import { useState } from "react";
import { Link } from "react-router-dom";
import { createPayment } from "../api/paymentsApi";
import { ApiErrorPanel } from "../components/ApiErrorPanel";
import { AuthNotice } from "../components/AuthNotice";
import { DemoAuthPanel } from "../components/DemoAuthPanel";
import { DemoIdentityPanel } from "../components/DemoIdentityPanel";
import { EmptyState, PageHeader, ProductCard, StatusPill, TechnicalDetails } from "../components/ProductUI";
import { WhatThisProves } from "../components/WhatThisProves";
import { useDemoIdentity, useDemoToken } from "../utils/demoStorage";
import type { CreatePaymentRequest, PaymentResponse } from "../types/api";

function createDefaults(): CreatePaymentRequest {
  const timestamp = Date.now();

  return {
    customerId: "customer-web-demo-001",
    amount: 125.5,
    currency: "USD",
    paymentReference: `WEB-DEMO-${timestamp}`,
  };
}

export function PaymentsPage() {
  const demoIdentity = useDemoIdentity();
  const { token } = useDemoToken();
  const [form, setForm] = useState<CreatePaymentRequest>(() => createDefaults());
  const [response, setResponse] = useState<PaymentResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  function updateField(field: keyof CreatePaymentRequest, value: string) {
    setForm((current) => ({
      ...current,
      [field]: field === "amount" ? Number(value) : value,
    }));
  }

  async function submitPayment() {
    setError(null);
    setLoading(true);

    const request = {
      ...form,
      tenantId: demoIdentity.tenantId,
      correlationId: demoIdentity.correlationId,
      idempotencyKey: demoIdentity.idempotencyKey,
    };

    try {
      setResponse(
        await createPayment(request, {
          tenantId: demoIdentity.tenantId,
          correlationId: demoIdentity.correlationId,
          idempotencyKey: demoIdentity.idempotencyKey,
          authorizationToken: token || undefined,
        }),
      );
      setForm(request);
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
        title="Payment Orchestration Control"
        subtitle="Show idempotent, tenant-aware payment execution with replay protection and evidence-ready correlation."
      />

      <DemoAuthPanel />
      <AuthNotice />
      <DemoIdentityPanel onIdentityChange={demoIdentity.setDemoIdentity} />

      <WhatThisProves
        title="What This Proves"
        points={[
          "Payment workflows are tenant-aware",
          "Idempotency prevents duplicate processing",
          "Payment actions can be traced through correlation ID",
        ]}
      />

      <ProductCard
        title="New Payment Workflow"
        subtitle="Submit a controlled payment request with tenant, correlation, and idempotency context."
      >
        <div className="form-grid">
          <label>
            Tenant ID
            <input value={demoIdentity.tenantId} readOnly />
          </label>
          <label>
            Customer ID
            <input value={form.customerId} onChange={(event) => updateField("customerId", event.target.value)} />
          </label>
          <label>
            Amount
            <input
              type="number"
              step="0.01"
              value={form.amount}
              onChange={(event) => updateField("amount", event.target.value)}
            />
          </label>
          <label>
            Currency
            <input value={form.currency} onChange={(event) => updateField("currency", event.target.value)} />
          </label>
          <label>
            Payment Reference
            <input
              value={form.paymentReference}
              onChange={(event) => updateField("paymentReference", event.target.value)}
            />
          </label>
          <label>
            Correlation ID
            <input value={demoIdentity.correlationId} readOnly />
          </label>
          <label>
            Idempotency Key
            <input value={demoIdentity.idempotencyKey} readOnly />
          </label>
        </div>
        <div className="button-row">
          <button type="button" onClick={submitPayment} disabled={loading}>
            Submit Payment
          </button>
          <button type="button" className="secondary-button" onClick={submitPayment} disabled={loading}>
            Replay Same Idempotency Key
          </button>
        </div>
        <ApiErrorPanel error={error} />
      </ProductCard>

      <ProductCard
        title="Payment Result Summary"
        subtitle="Business-readable result before technical response details."
        badge={<StatusPill status={response?.status ?? response?.decision ?? "UNKNOWN"} />}
      >
        {response ? (
          <div className="summary-grid">
            <div>
              <span>Payment ID</span>
              <strong>{response.paymentId ?? response.id ?? "-"}</strong>
            </div>
            <div>
              <span>Decision</span>
              <strong>{response.decision ?? response.status ?? "-"}</strong>
            </div>
            <div>
              <span>Reference</span>
              <strong>{response.paymentReference ?? form.paymentReference}</strong>
            </div>
            <div>
              <span>Replay / Duplicate Indicator</span>
              <strong>{String(response.replayed ?? response.duplicate ?? response.idempotentReplay ?? "Not reported")}</strong>
            </div>
            <div>
              <span>Correlation ID</span>
              <strong>{response.correlationId ?? demoIdentity.correlationId}</strong>
            </div>
          </div>
        ) : (
          <EmptyState
            title="No payment response yet."
            message="Submit a payment workflow to test orchestration and idempotency behavior."
          />
        )}
        <TechnicalDetails data={response} />
      </ProductCard>

      <ProductCard title="Evidence Link" subtitle="Use the payment context to inspect compliance evidence.">
        <p>
          Payment evidence is searched by correlation ID or payment ID in the Compliance Evidence Ledger.
        </p>
        <Link className="button-link secondary-link" to="/evidence">
          View evidence for this payment
        </Link>
      </ProductCard>
    </section>
  );
}
