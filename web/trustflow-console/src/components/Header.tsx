import { LoginStatus } from "./LoginStatus";
import { useAuth } from "../auth/useAuth";
import { useDemoToken } from "../utils/demoStorage";
import { useLocation } from "react-router-dom";

const routeMeta: Record<string, { title: string; subtitle: string }> = {
  "/dashboard": {
    title: "Trust Operations Dashboard",
    subtitle: "Commercial overview of controlled, tenant-aware, evidence-backed workflow execution.",
  },
  "/trust-flow-demo": {
    title: "Audit-Ready Trust Flow Demo",
    subtitle: "Prove identity, document processing, grounded knowledge, payment orchestration, and evidence capture.",
  },
  "/documents": {
    title: "Document Intelligence Control",
    subtitle: "Controlled extraction with tenant, correlation, and document traceability.",
  },
  "/knowledge": {
    title: "Grounded Knowledge Control",
    subtitle: "Enterprise AI answers packaged with grounding, citations, and workflow context.",
  },
  "/payments": {
    title: "Payment Orchestration Control",
    subtitle: "Tenant-aware payment execution with idempotency and replay protection.",
  },
  "/evidence": {
    title: "Compliance Evidence Ledger",
    subtitle: "Audit trail for workflow decisions, risk posture, services, and correlation IDs.",
  },
  "/health": {
    title: "Platform Operations Health",
    subtitle: "Production-readiness view of gateway reachability, auth posture, and service availability.",
  },
  "/billing": {
    title: "Billing",
    subtitle: "Commercial roadmap view for usage metering and enterprise billing controls.",
  },
  "/demo-script": {
    title: "Demo Script",
    subtitle: "Presenter talk track for business users, enterprise buyers, and technical reviewers.",
  },
  "/auth-debug": {
    title: "Auth Diagnostics",
    subtitle: "Admin view of Keycloak session state, realm configuration, and token availability.",
  },
};

export function Header() {
  const location = useLocation();
  const auth = useAuth();
  const { token } = useDemoToken();
  const meta = routeMeta[location.pathname] ?? routeMeta["/dashboard"];
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
  const authState = auth.isAuthenticated
    ? "Keycloak Session Active"
    : token
      ? "Demo Token Fallback"
      : "No Token Active";

  return (
    <header className="top-header">
      <div className="header-title">
        <span className="header-kicker">NAITFS Demo Environment · API Gateway: {apiBaseUrl}</span>
        <h1>{meta.title}</h1>
        <p>{meta.subtitle}</p>
      </div>
      <div className="header-meta">
        <span className={`session-indicator ${auth.isAuthenticated ? "active" : token ? "fallback" : ""}`}>
          {authState}
        </span>
      </div>
      <LoginStatus />
    </header>
  );
}
