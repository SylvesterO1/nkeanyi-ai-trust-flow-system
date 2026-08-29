import type { ReactNode } from "react";
import { StatusBadge } from "./StatusBadge";
import { JsonViewer } from "./JsonViewer";
import type { EvidenceRecord } from "../types/api";

type PageHeaderProps = {
  eyebrow?: string;
  title: string;
  subtitle: string;
  children?: ReactNode;
};

export function PageHeader({ children, eyebrow = "Demo Environment", subtitle, title }: PageHeaderProps) {
  return (
    <section className="page-header">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
      {children && <div className="page-header-actions">{children}</div>}
    </section>
  );
}

type ProductCardProps = {
  badge?: ReactNode;
  children: ReactNode;
  className?: string;
  subtitle?: string;
  title?: string;
};

export function ProductCard({ badge, children, className = "", subtitle, title }: ProductCardProps) {
  return (
    <section className={`panel product-card ${className}`.trim()}>
      {(title || subtitle || badge) && (
        <div className="panel-header">
          <div>
            {title && <h3>{title}</h3>}
            {subtitle && <p>{subtitle}</p>}
          </div>
          {badge}
        </div>
      )}
      {children}
    </section>
  );
}

type MetricCardProps = {
  label: string;
  value: string;
  helperText?: string;
  badge?: string;
};

export function MetricCard({ badge, helperText, label, value }: MetricCardProps) {
  return (
    <article className="stat-card metric-card">
      {badge && <span className="metric-badge">{badge}</span>}
      <span>{label}</span>
      <strong>{value}</strong>
      {helperText && <p>{helperText}</p>}
    </article>
  );
}

export function StatusPill({ status = "UNKNOWN" }: { status?: string }) {
  return <StatusBadge status={status} />;
}

type TrustControlCardProps = {
  control: string;
  description: string;
  status?: string;
};

export function TrustControlCard({ control, description, status = "ACTIVE" }: TrustControlCardProps) {
  return (
    <article className="trust-control-card">
      <StatusPill status={status} />
      <strong>{control}</strong>
      <p>{description}</p>
    </article>
  );
}

type WorkflowStepperProps = {
  steps: Array<{
    title: string;
    functionLabel: string;
    control: string;
    status?: string;
  }>;
};

export function WorkflowStepper({ steps }: WorkflowStepperProps) {
  return (
    <div className="workflow-stepper">
      {steps.map((step, index) => (
        <article key={step.title} className="workflow-node">
          <span className="step-number">{index + 1}</span>
          <div>
            <strong>{step.title}</strong>
            <p>{step.functionLabel}</p>
            <small>{step.control}</small>
          </div>
          <StatusPill status={step.status ?? "ACTIVE"} />
        </article>
      ))}
    </div>
  );
}

type CommercialStepCardProps = {
  action?: ReactNode;
  children?: ReactNode;
  control: string;
  purpose: string;
  status?: string;
  stepNumber: number;
  title: string;
};

export function CommercialStepCard({
  action,
  children,
  control,
  purpose,
  status = "NOT_STARTED",
  stepNumber,
  title,
}: CommercialStepCardProps) {
  return (
    <section className="panel commercial-step-card">
      <div className="panel-header">
        <div className="step-heading">
          <span className="step-number">{stepNumber}</span>
          <div>
            <h3>{title}</h3>
            <p>{purpose}</p>
          </div>
        </div>
        <StatusPill status={status} />
      </div>
      <div className="control-proof">
        <span>Control being proven</span>
        <strong>{control}</strong>
      </div>
      {action && <div className="button-row">{action}</div>}
      {children}
    </section>
  );
}

export function TechnicalDetails({ data, title = "Technical response" }: { data: unknown; title?: string }) {
  return <JsonViewer data={data} title={title} />;
}

export function ApiResponsePanel({ data }: { data: unknown }) {
  return <TechnicalDetails data={data} title="Technical response" />;
}

export function EmptyState({ action, message, title }: { action?: ReactNode; message: string; title: string }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <p>{message}</p>
      {action && <div className="button-row">{action}</div>}
    </div>
  );
}

function timestampOf(record: EvidenceRecord) {
  return record.createdAt ?? record.timestamp ?? "";
}

export function EvidenceTimeline({ records }: { records: EvidenceRecord[] }) {
  if (records.length === 0) {
    return (
      <EmptyState
        title="No evidence loaded yet."
        message="Run a Trust Flow Demo or search by correlation ID to view audit evidence."
      />
    );
  }

  return (
    <ol className="evidence-timeline">
      {records.map((record, index) => (
        <li key={record.id ?? `${record.correlationId}-${index}`}>
          <div className="timeline-dot" />
          <article>
            <div className="timeline-card-header">
              <strong>{record.evidenceType ?? "Evidence event"}</strong>
              <StatusPill status={record.decision ?? "RECEIVED"} />
            </div>
            <p>
              {record.sourceService ?? "NAITFS service"} recorded evidence for correlation{" "}
              {record.correlationId ?? "pending"}.
            </p>
            <div className="timeline-facts">
              <span>Risk: {record.riskLevel ?? "UNKNOWN"}</span>
              <span>{timestampOf(record) || "Timestamp pending"}</span>
              <span>{record.paymentId ? `Payment: ${record.paymentId}` : "Payment: not attached"}</span>
            </div>
          </article>
        </li>
      ))}
    </ol>
  );
}
