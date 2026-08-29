import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { getDocumentById, processDocument } from "../api/documentsApi";
import { getEvidenceByCorrelationId } from "../api/evidenceApi";
import { getGatewayHealth } from "../api/healthApi";
import { generateKnowledgeAnswer, indexKnowledgeText } from "../api/knowledgeApi";
import { createPayment } from "../api/paymentsApi";
import { ApiErrorPanel } from "../components/ApiErrorPanel";
import { AuthNotice } from "../components/AuthNotice";
import { DemoAuthPanel } from "../components/DemoAuthPanel";
import { DemoChecklist } from "../components/DemoChecklist";
import { DemoIdentityPanel } from "../components/DemoIdentityPanel";
import { JsonViewer } from "../components/JsonViewer";
import { PageHeader, ProductCard } from "../components/ProductUI";
import { StatusBadge } from "../components/StatusBadge";
import { TrustTimeline } from "../components/TrustTimeline";
import { WhatThisProves } from "../components/WhatThisProves";
import { useDemoIdentity, useDemoToken } from "../utils/demoStorage";
import type {
  CreatePaymentRequest,
  DocumentProcessResponse,
  DocumentRecord,
  EvidenceSearchResponse,
  HealthResponse,
  KnowledgeAnswerResponse,
  KnowledgeIndexResponse,
  PaymentResponse,
} from "../types/api";
import { extractEvidenceRecords } from "./evidenceUtils";

type StepStatus = "NOT_STARTED" | "RUNNING" | "SUCCESS" | "FAILED" | "COMPLETED";

function getDocumentId(response: DocumentProcessResponse | null) {
  return response?.documentId ?? response?.objectName ?? "";
}

function getPaymentId(response: PaymentResponse | null) {
  return response?.paymentId ?? response?.id ?? "";
}

function StepCard({
  children,
  control,
  description,
  stepNumber,
  status,
  title,
}: {
  children: ReactNode;
  control: string;
  description: string;
  stepNumber: number;
  status: StepStatus;
  title: string;
}) {
  return (
    <section className="panel step-panel">
      <div className="panel-header">
        <div className="step-heading">
          <span className="step-number">{stepNumber}</span>
          <div>
            <h3>{title}</h3>
            <p>{description}</p>
          </div>
        </div>
        <StatusBadge status={status} />
      </div>
      <div className="control-proof">
        <span>Control being proven</span>
        <strong>{control}</strong>
      </div>
      {children}
    </section>
  );
}

function StepTechnicalDetails({ children }: { children: ReactNode }) {
  return <div className="step-technical-details">{children}</div>;
}

function ResultSummary({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="result-line">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

export function TrustFlowDemoPage() {
  const demoIdentity = useDemoIdentity();
  const { token } = useDemoToken();
  const [documentFile, setDocumentFile] = useState<File | null>(null);
  const [indexedText, setIndexedText] = useState(
    "NAITFS provides traceable workflow evidence for regulated document, knowledge, payment, and compliance operations.",
  );
  const [question, setQuestion] = useState("What business workflow does this document support?");
  const [documentResponse, setDocumentResponse] = useState<DocumentProcessResponse | null>(null);
  const [documentRecord, setDocumentRecord] = useState<DocumentRecord | null>(null);
  const [indexResponse, setIndexResponse] = useState<KnowledgeIndexResponse | null>(null);
  const [knowledgeResponse, setKnowledgeResponse] = useState<KnowledgeAnswerResponse | null>(null);
  const [paymentResponse, setPaymentResponse] = useState<PaymentResponse | null>(null);
  const [evidenceResponse, setEvidenceResponse] = useState<EvidenceSearchResponse | null>(null);
  const [healthResponse, setHealthResponse] = useState<HealthResponse | null>(null);
  const [idempotencyReplayTested, setIdempotencyReplayTested] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [stepStatus, setStepStatus] = useState<Record<string, StepStatus>>({
    document: "NOT_STARTED",
    knowledge: "NOT_STARTED",
    payment: "NOT_STARTED",
    evidence: "NOT_STARTED",
    health: "NOT_STARTED",
  });

  const records = extractEvidenceRecords(evidenceResponse);
  const documentId = getDocumentId(documentResponse);
  const documentRecordStatus = documentRecord?.status ?? "Pending";
  const paymentId = getPaymentId(paymentResponse);
  const paymentStatus = paymentResponse?.status ?? paymentResponse?.decision ?? "Pending";
  const evidenceCount = records.length;
  const finalDecision = records[0]?.decision ?? paymentStatus;
  const riskLevel = records[0]?.riskLevel ?? "Not reported";
  const workflowComplete = Boolean(documentId && paymentId && evidenceCount > 0);
  const canRunKnowledge = Boolean(question && (documentId || indexedText));
  const canRunEvidence = Boolean(demoIdentity.correlationId);
  const hasSuccessfulBackendStep = ["document", "knowledge", "payment", "evidence"].some(
    (step) => stepStatus[step] === "SUCCESS",
  );

  const requestOptions = useMemo(
    () => ({
      tenantId: demoIdentity.tenantId,
      correlationId: demoIdentity.correlationId,
      idempotencyKey: demoIdentity.idempotencyKey,
      authorizationToken: token || undefined,
    }),
    [demoIdentity.correlationId, demoIdentity.idempotencyKey, demoIdentity.tenantId, token],
  );

  const paymentRequest: CreatePaymentRequest = {
    tenantId: demoIdentity.tenantId,
    correlationId: demoIdentity.correlationId,
    customerId: "customer-web-demo-001",
    amount: 125.5,
    currency: "USD",
    paymentReference: `WEB-DEMO-${demoIdentity.correlationId}`,
    idempotencyKey: demoIdentity.idempotencyKey,
  };

  function setStatus(step: string, status: StepStatus) {
    setStepStatus((current) => ({ ...current, [step]: status }));
  }

  async function runWithStatus(step: string, action: () => Promise<void>) {
    setError(null);
    setStatus(step, "RUNNING");

    try {
      await action();
      setStatus(step, "SUCCESS");
    } catch (err) {
      setStatus(step, "FAILED");
      setError(err);
    }
  }

  async function checkHealth() {
    await runWithStatus("health", async () => {
      setHealthResponse(await getGatewayHealth());
    });
  }

  async function runDocumentStep() {
    if (!documentFile) {
      setError(new Error("Choose a document file before running the document step."));
      return;
    }

    await runWithStatus("document", async () => {
      const result = await processDocument(documentFile, requestOptions);
      setDocumentResponse(result);
      setDocumentRecord(null);
    });
  }

  async function fetchDocumentRecord() {
    if (!documentId) {
      setError(new Error("Process a document before fetching its persisted record."));
      return;
    }

    await runWithStatus("document", async () => {
      setDocumentRecord(await getDocumentById(documentId, requestOptions));
    });
  }

  async function runKnowledgeStep() {
    await runWithStatus("knowledge", async () => {
      if (!documentId && indexedText) {
        setIndexResponse(
          await indexKnowledgeText(
            {
              content: indexedText,
              source: `web-demo-${demoIdentity.correlationId}`,
            },
            requestOptions,
          ),
        );
      }

      setKnowledgeResponse(
        await generateKnowledgeAnswer(
          {
            query: question,
            documentId: documentId || `web-demo-${demoIdentity.correlationId}`,
          },
          requestOptions,
        ),
      );
    });
  }

  async function runPaymentStep(replay = false) {
    await runWithStatus("payment", async () => {
      setPaymentResponse(await createPayment(paymentRequest, requestOptions));
      if (replay) {
        setIdempotencyReplayTested(true);
      }
    });
  }

  async function runEvidenceStep() {
    await runWithStatus("evidence", async () => {
      setEvidenceResponse(await getEvidenceByCorrelationId(demoIdentity.correlationId, requestOptions));
    });
  }

  function resetDemoContext() {
    demoIdentity.generateNewIdentity();
    setDocumentFile(null);
    setDocumentResponse(null);
    setDocumentRecord(null);
    setIndexResponse(null);
    setKnowledgeResponse(null);
    setPaymentResponse(null);
    setEvidenceResponse(null);
    setHealthResponse(null);
    setIdempotencyReplayTested(false);
    setError(null);
    setStepStatus({
      document: "NOT_STARTED",
      knowledge: "NOT_STARTED",
      payment: "NOT_STARTED",
      evidence: "NOT_STARTED",
      health: "NOT_STARTED",
    });
  }

  const checklist = [
    { done: healthResponse?.status === "UP", label: "API Gateway UP" },
    { done: stepStatus.document === "SUCCESS", label: "Document processed" },
    { done: stepStatus.knowledge === "SUCCESS", label: "Knowledge answer generated" },
    { done: stepStatus.payment === "SUCCESS", label: "Payment submitted" },
    { done: idempotencyReplayTested, label: "Idempotency replay tested" },
    { done: stepStatus.evidence === "SUCCESS", label: "Evidence searched by correlationId" },
    { done: evidenceCount > 0, label: "Trust timeline displayed" },
  ];

  return (
    <section className="page-stack">
      <PageHeader
        title="Audit-Ready Trust Flow Demo"
        subtitle="Run a controlled workflow that proves identity, document processing, grounded knowledge, payment orchestration, idempotency, and compliance evidence capture."
      />

      <ProductCard title="Regulated Business Workflow" subtitle="Buyer-facing scenario for a controlled enterprise workflow.">
        <div className="scenario-grid">
          <div>
            <span>Business goal</span>
            <p>Process a document, generate a grounded answer, execute a payment workflow, and capture audit evidence.</p>
          </div>
          <div>
            <span>Controls demonstrated</span>
            <p>JWT authentication, tenant propagation, correlation ID, idempotency key, evidence capture, and service health.</p>
          </div>
          <div>
            <span>Commercial proof</span>
            <p>Shows that regulated workflows can be traceable, controlled, tenant-aware, and evidence-backed.</p>
          </div>
        </div>
      </ProductCard>

      <div className="content-grid two-column">
        <DemoIdentityPanel onIdentityChange={demoIdentity.setDemoIdentity} title="Step 1: Verify Identity & Tenant Context" />
        <div className="content-grid">
          <DemoAuthPanel compact />
          <AuthNotice />
        </div>
      </div>

      <DemoChecklist items={checklist} />
      <div className="button-row standalone-actions">
        <button type="button" className="secondary-button" onClick={checkHealth}>
          Check API Gateway
        </button>
        <button type="button" className="secondary-button" onClick={resetDemoContext}>
          Reset Demo Context
        </button>
      </div>

      <div className="content-grid two-column">
        <StepCard
          control="Document ID, tenant propagation, and correlation traceability"
          description="Upload and process a business document through the API Gateway."
          stepNumber={2}
          status={stepStatus.document}
          title="Process Document"
        >
          <label>
            Document File
            <input type="file" onChange={(event) => setDocumentFile(event.target.files?.[0] ?? null)} />
          </label>
          <div className="button-row">
            <button type="button" onClick={runDocumentStep} disabled={stepStatus.document === "RUNNING"}>
              Process Demo Document
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={fetchDocumentRecord}
              disabled={stepStatus.document === "RUNNING" || !documentId}
            >
              View Document Record
            </button>
          </div>
          <ResultSummary label="Document ID" value={documentId || "Pending"} />
          <ResultSummary label="Document Record Status" value={documentRecordStatus} />
          <StepTechnicalDetails>
            <JsonViewer data={documentRecord ?? documentResponse} />
          </StepTechnicalDetails>
        </StepCard>

        <StepCard
          control="Grounded answer generation with reviewable citation context"
          description="Ask a grounded question and preserve citation context for review."
          stepNumber={3}
          status={stepStatus.knowledge}
          title="Generate Grounded Knowledge"
        >
          <label>
            Indexed Text Fallback
            <textarea value={indexedText} onChange={(event) => setIndexedText(event.target.value)} rows={3} />
          </label>
          <label>
            Question
            <textarea value={question} onChange={(event) => setQuestion(event.target.value)} rows={3} />
          </label>
          <div className="button-row">
            <button type="button" onClick={runKnowledgeStep} disabled={stepStatus.knowledge === "RUNNING" || !canRunKnowledge}>
              Generate Grounded Answer
            </button>
          </div>
          <div className="answer-box">{knowledgeResponse?.answer || "No answer generated yet."}</div>
          <StepTechnicalDetails>
            <JsonViewer data={knowledgeResponse ?? indexResponse} />
          </StepTechnicalDetails>
        </StepCard>
      </div>

      <div className="content-grid two-column">
        <StepCard
          control="Idempotency key and duplicate payment protection"
          description="Create a payment with the same demo tenant, correlation, and idempotency values."
          stepNumber={4}
          status={stepStatus.payment}
          title="Execute Payment Workflow"
        >
          <div className="button-row">
            <button type="button" onClick={() => runPaymentStep(false)} disabled={stepStatus.payment === "RUNNING"}>
              Submit Demo Payment
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={() => runPaymentStep(true)}
              disabled={stepStatus.payment === "RUNNING"}
            >
              Replay Same Idempotency Key
            </button>
          </div>
          <div className="summary-grid compact-summary">
            <div>
              <span>paymentId</span>
              <strong>{paymentId || "Pending"}</strong>
            </div>
            <div>
              <span>Status</span>
              <strong>{paymentStatus}</strong>
            </div>
            <div>
              <span>Replay Control</span>
              <strong>
                {idempotencyReplayTested
                  ? "Replay tested with the same idempotency key; duplicate execution is controlled."
                  : "Submit once, then replay with the same idempotency key."}
              </strong>
            </div>
          </div>
          <StepTechnicalDetails>
            <JsonViewer data={paymentResponse} />
          </StepTechnicalDetails>
        </StepCard>

        <StepCard
          control="Compliance evidence capture by shared correlation ID"
          description="Search compliance evidence recorded against the shared correlation ID."
          stepNumber={5}
          status={stepStatus.evidence}
          title="Capture Compliance Evidence"
        >
          <div className="button-row">
            <button type="button" onClick={runEvidenceStep} disabled={stepStatus.evidence === "RUNNING" || !canRunEvidence}>
              Search Evidence by Correlation ID
            </button>
          </div>
          <div className="result-line">
            <span>evidenceCount</span>
            <strong>{evidenceCount}</strong>
          </div>
          <StepTechnicalDetails>
            <JsonViewer data={evidenceResponse} />
          </StepTechnicalDetails>
        </StepCard>
      </div>

      <ApiErrorPanel error={error} />

      <StepCard
        control="End-to-end audit readiness across tenant, workflow, payment, and evidence context"
        description="Executive proof that the workflow can be repeated, traced, and audited."
        stepNumber={6}
        status={workflowComplete ? "COMPLETED" : hasSuccessfulBackendStep ? "RUNNING" : "NOT_STARTED"}
        title="Audit-Ready Workflow Complete"
      >
        <div className="summary-grid final-summary-grid">
          <div>
            <span>Workflow Status</span>
            <strong>{workflowComplete ? "Complete" : "Pending required evidence"}</strong>
          </div>
          <div>
            <span>Tenant ID</span>
            <strong>{demoIdentity.tenantId}</strong>
          </div>
          <div>
            <span>Correlation ID</span>
            <strong>{demoIdentity.correlationId}</strong>
          </div>
          <div>
            <span>Document ID</span>
            <strong>{documentId || "Pending"}</strong>
          </div>
          <div>
            <span>Payment ID</span>
            <strong>{paymentId || "Pending"}</strong>
          </div>
          <div>
            <span>Idempotency Key</span>
            <strong>{demoIdentity.idempotencyKey}</strong>
          </div>
          <div>
            <span>Evidence Count</span>
            <strong>{evidenceCount}</strong>
          </div>
          <div>
            <span>Final Decision</span>
            <strong>{finalDecision}</strong>
          </div>
          <div>
            <span>Risk Level</span>
            <strong>{riskLevel}</strong>
          </div>
          <div>
            <span>Services Involved</span>
            <strong>Gateway, Documents, Knowledge, Payments, Evidence</strong>
          </div>
        </div>
        <div className="final-message">
          NkeanyiAI TrustFlow produced traceable evidence across identity, tenant context, document intelligence,
          grounded knowledge, payment orchestration, and compliance capture.
        </div>
        <p className="commercial-summary">
          This workflow produced traceable evidence suitable for compliance review, incident analysis, and production-risk governance.
        </p>
        <div className="button-row">
          <Link className="button-link" to="/evidence">
            View Compliance Evidence
          </Link>
          <button type="button" className="secondary-button" onClick={resetDemoContext}>
            Run Another Demo
          </button>
          <Link className="button-link secondary-link" to="/demo-script">
            Open Demo Script
          </Link>
        </div>
      </StepCard>

      <section className="panel">
        <h3>Trust Timeline</h3>
        <TrustTimeline records={records} />
      </section>

      <WhatThisProves
        title="What This Proves"
        points={[
          "The full workflow can be demonstrated end to end",
          "One tenant and correlation ID connect all steps",
          "Evidence creates a defensible audit trail",
        ]}
      />
    </section>
  );
}
