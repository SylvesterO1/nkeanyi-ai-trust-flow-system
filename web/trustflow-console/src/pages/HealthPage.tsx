import { useState } from "react";
import { getGatewayHealth } from "../api/healthApi";
import { ApiErrorPanel } from "../components/ApiErrorPanel";
import { EmptyState, PageHeader, ProductCard, StatusPill, TechnicalDetails } from "../components/ProductUI";
import { StatusBadge } from "../components/StatusBadge";
import type { HealthResponse } from "../types/api";

const serviceNames = [
  "API Gateway",
  "Document Intelligence",
  "Knowledge Service",
  "Payment Orchestration",
  "Compliance Evidence",
  "Keycloak",
];

export function HealthPage() {
  const [status, setStatus] = useState("UNKNOWN");
  const [response, setResponse] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<unknown>(null);

  async function checkHealth() {
    setError(null);
    setStatus("CHECKING");

    try {
      const result = await getGatewayHealth();
      setResponse(result);
      setStatus(result.status || "UNKNOWN");
    } catch (err) {
      setResponse(null);
      setStatus("DOWN");
      setError(err);
    }
  }

  return (
    <section className="page-stack">
      <PageHeader
        title="Platform Operations Health"
        subtitle="Production-readiness view of gateway reachability, authentication posture, and service availability."
      />

      <ProductCard title="Gateway Reachability" subtitle="Confirm the console can reach the NAITFS API Gateway.">
        <div className="panel-header">
          <div>
            <p>Gateway target: {import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"}</p>
          </div>
          <StatusBadge status={status} />
        </div>
        <button type="button" onClick={checkHealth}>
          Check API Gateway Health
        </button>
        <ApiErrorPanel error={error} />
      </ProductCard>

      <ProductCard title="Service Status Grid" subtitle="Commercial demo view of core platform components.">
        <div className="service-status-grid">
          {serviceNames.map((service) => (
            <article key={service}>
              <span>{service}</span>
              <StatusPill status={service === "API Gateway" ? status : response ? "UNKNOWN" : "NOT_STARTED"} />
            </article>
          ))}
        </div>
      </ProductCard>

      <ProductCard title="Readiness Panel" subtitle="Signals an enterprise reviewer expects before trusting a workflow demo.">
        {response ? (
          <div className="summary-grid">
            <div>
              <span>Gateway Status</span>
              <strong>{response.status}</strong>
            </div>
            <div>
              <span>Components Reported</span>
              <strong>{response.components ? Object.keys(response.components).length : 0}</strong>
            </div>
            <div>
              <span>Target</span>
              <strong>{import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"}</strong>
            </div>
            <div>
              <span>Auth Configured</span>
              <strong>Keycloak realm payment-platform</strong>
            </div>
            <div>
              <span>Evidence Pipeline</span>
              <strong>{status === "UP" ? "Ready for workflow evidence" : "Pending gateway check"}</strong>
            </div>
          </div>
        ) : (
          <EmptyState
            title="No health response yet."
            message="Check the API Gateway to confirm local platform services are reachable."
          />
        )}
        <TechnicalDetails data={response} title="Health endpoint response" />
      </ProductCard>
    </section>
  );
}
