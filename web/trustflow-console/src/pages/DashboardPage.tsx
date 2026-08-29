import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getRecentDocumentRecords } from "../api/documentsApi";
import { ApiErrorPanel } from "../components/ApiErrorPanel";
import { DocumentRecordsTable } from "../components/DocumentRecordsTable";
import { JsonViewer } from "../components/JsonViewer";
import {
  EmptyState,
  MetricCard,
  ProductCard,
  TrustControlCard,
  WorkflowStepper,
} from "../components/ProductUI";
import { useDemoIdentity, useDemoToken } from "../utils/demoStorage";
import type { DocumentRecord } from "../types/api";

const stats = [
  ["Workflows Controlled", "1,248", "Demo environment", "Tenant-aware execution"],
  ["Evidence Events Captured", "2,604", "Demo environment", "Audit records indexed"],
  ["Duplicate Requests Prevented", "316", "Demo environment", "Idempotency protection"],
  ["Services Monitored", "5", "Demo environment", "Gateway plus core services"],
  ["Audit Readiness", "High", "Demo environment", "Traceable evidence path"],
];

const posture = [
  "Identity enforced",
  "Tenant isolation active",
  "Correlation tracking active",
  "Idempotency controls active",
  "Evidence capture active",
];

const workflowNodes = [
  {
    title: "User / App",
    functionLabel: "Business request starts",
    control: "Authenticated entry point",
  },
  {
    title: "API Gateway",
    functionLabel: "Policy and routing layer",
    control: "JWT and tenant propagation",
  },
  {
    title: "Document Intelligence",
    functionLabel: "Document extraction",
    control: "Document ID and correlation",
  },
  {
    title: "Knowledge Service",
    functionLabel: "Grounded answer",
    control: "Citation-backed response",
  },
  {
    title: "Payment Orchestration",
    functionLabel: "Business execution",
    control: "Idempotency key",
  },
  {
    title: "Compliance Evidence",
    functionLabel: "Audit trail",
    control: "Evidence event capture",
  },
];

const controls = [
  ["Tenant Boundary Control", "Keeps customer workflow context scoped to the correct tenant."],
  ["Correlation ID Traceability", "Connects cross-service activity into one reviewable chain."],
  ["Idempotent Payment Protection", "Reduces duplicate payment execution during retries."],
  ["Evidence Chain", "Records decisions and service activity for compliance review."],
  ["JWT Enforcement", "Preserves identity context before business workflow execution."],
];

export function DashboardPage() {
  const demoIdentity = useDemoIdentity();
  const { token } = useDemoToken();
  const [documentRecords, setDocumentRecords] = useState<DocumentRecord[]>([]);
  const [selectedRecord, setSelectedRecord] = useState<DocumentRecord | null>(null);
  const [documentActivityError, setDocumentActivityError] = useState<unknown>(null);

  const requestOptions = useMemo(
    () => ({
      authorizationToken: token || undefined,
      limit: 5,
      tenantId: demoIdentity.tenantId,
    }),
    [demoIdentity.tenantId, token],
  );

  useEffect(() => {
    let active = true;

    getRecentDocumentRecords(requestOptions)
      .then((response) => {
        if (active) {
          setDocumentRecords(response.records.slice(0, 5));
          setDocumentActivityError(null);
        }
      })
      .catch((error) => {
        if (active) {
          setDocumentRecords([]);
          setDocumentActivityError(error);
        }
      });

    return () => {
      active = false;
    };
  }, [requestOptions]);

  return (
    <section className="page-stack">
      <section className="executive-hero">
        <div>
          <span className="eyebrow">NkeanyiAI TrustFlow · NAITFS</span>
          <h2>Regulated AI Workflow Trust, From Request to Evidence</h2>
          <p className="hero-subtitle">
            NAITFS gives enterprise teams traceability, idempotency, tenant isolation, and audit evidence
            across AI-assisted document, knowledge, and payment workflows.
          </p>
          <p>
            Designed for buyer walkthroughs, compliance reviews, and engineering leadership: every workflow
            should produce a controlled execution trail before raw logs are needed.
          </p>
          <div className="hero-actions horizontal-actions">
            <Link className="button-link" to="/trust-flow-demo">
              Run Trust Flow Demo
            </Link>
            <Link className="button-link secondary-link" to="/evidence">
              View Audit Evidence
            </Link>
          </div>
        </div>
        <div className="trust-controls-card">
          <span className="demo-data-badge">Demo Environment</span>
          <h3>Trust posture</h3>
          <ul>
            {posture.map((control) => (
              <li key={control}>
                <span />
                {control}
              </li>
            ))}
          </ul>
        </div>
      </section>

      <div className="trust-posture-strip">
        {posture.map((item) => (
          <span key={item}>{item}</span>
        ))}
      </div>

      <div className="stats-grid">
        {stats.map(([label, value, badge, helperText]) => (
          <MetricCard key={label} label={label} value={value} badge={badge} helperText={helperText} />
        ))}
      </div>

      <ProductCard
        title="Trust Flow Map"
        subtitle="A buyer-readable view of how one request becomes controlled, traceable evidence."
      >
        <WorkflowStepper steps={workflowNodes} />
      </ProductCard>

      <div className="trust-control-grid">
        {controls.map(([control, description]) => (
          <TrustControlCard key={control} control={control} description={description} />
        ))}
      </div>

      <ProductCard title="Executive Summary" subtitle="Why this matters commercially.">
        <p className="commercial-summary">
          Every regulated workflow should answer: who initiated it, which tenant it belonged to, what changed,
          which services handled it, whether it was replayed, and what evidence was recorded.
        </p>
      </ProductCard>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h3>Recent Workflow Evidence</h3>
            <p>Latest persisted document records for the current demo tenant.</p>
          </div>
        </div>
        {documentActivityError ? (
          <>
            <EmptyState
              title="Document activity is unavailable."
              message="Likely cause: the backend service is not reachable. Start the platform, then refresh this dashboard."
            />
            <ApiErrorPanel error={documentActivityError} />
          </>
        ) : (
          <DocumentRecordsTable records={documentRecords} onSelect={setSelectedRecord} />
        )}
        {selectedRecord && <JsonViewer data={selectedRecord} />}
      </section>
    </section>
  );
}
