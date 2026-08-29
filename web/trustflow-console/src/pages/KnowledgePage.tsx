import { useState } from "react";
import { generateKnowledgeAnswer, indexKnowledgeText } from "../api/knowledgeApi";
import { ApiErrorPanel } from "../components/ApiErrorPanel";
import { AuthNotice } from "../components/AuthNotice";
import { DemoAuthPanel } from "../components/DemoAuthPanel";
import { DemoIdentityPanel } from "../components/DemoIdentityPanel";
import { JsonViewer } from "../components/JsonViewer";
import { EmptyState, PageHeader, ProductCard, StatusPill, TechnicalDetails } from "../components/ProductUI";
import { WhatThisProves } from "../components/WhatThisProves";
import { useDemoIdentity, useDemoToken } from "../utils/demoStorage";
import type { KnowledgeAnswerResponse, KnowledgeIndexResponse } from "../types/api";

export function KnowledgePage() {
  const demoIdentity = useDemoIdentity();
  const { token } = useDemoToken();
  const [documentId, setDocumentId] = useState(() => `web-demo-document-${Date.now()}`);
  const [demoText, setDemoText] = useState(
    "NAITFS provides traceable workflow evidence for regulated document, knowledge, payment, and compliance operations.",
  );
  const [question, setQuestion] = useState("What does NAITFS make traceable?");
  const [indexResponse, setIndexResponse] = useState<KnowledgeIndexResponse | null>(null);
  const [answerResponse, setAnswerResponse] = useState<KnowledgeAnswerResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  const authOptions = {
    tenantId: demoIdentity.tenantId,
    correlationId: demoIdentity.correlationId,
    idempotencyKey: demoIdentity.idempotencyKey,
    authorizationToken: token || undefined,
  };

  async function indexText() {
    setError(null);
    setLoading(true);
    try {
      setIndexResponse(await indexKnowledgeText({ source: documentId, content: demoText }, authOptions));
    } catch (err) {
      setIndexResponse(null);
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  async function askQuestion() {
    setError(null);
    setLoading(true);
    try {
      setAnswerResponse(
        await generateKnowledgeAnswer(
          {
            query: question,
            documentId,
          },
          authOptions,
        ),
      );
    } catch (err) {
      setAnswerResponse(null);
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  const citations = answerResponse?.citationPackage?.citations ?? [];

  return (
    <section className="page-stack">
      <PageHeader
        title="Grounded Knowledge Control"
        subtitle="Generate enterprise AI answers with document grounding, citations, tenant context, and correlation traceability."
      />

      <DemoAuthPanel />
      <AuthNotice />
      <DemoIdentityPanel onIdentityChange={demoIdentity.setDemoIdentity} />

      <WhatThisProves
        title="What This Proves"
        points={[
          "AI answers are grounded in indexed document text",
          "Citations and metadata support trust",
          "Knowledge events can produce compliance evidence",
        ]}
      />

      <ProductCard title="Ask a Grounded Question" subtitle="Index controlled context, then generate an answer that can be reviewed.">
        <div className="form-grid">
          <label>
            Document ID
            <input value={documentId} onChange={(event) => setDocumentId(event.target.value)} />
          </label>
          <label className="wide-field">
            Demo Text
            <textarea value={demoText} onChange={(event) => setDemoText(event.target.value)} rows={5} />
          </label>
          <label className="wide-field">
            Question
            <textarea value={question} onChange={(event) => setQuestion(event.target.value)} rows={3} />
          </label>
        </div>
        <div className="button-row">
          <button type="button" onClick={indexText} disabled={loading || !demoText || !documentId}>
            Index Demo Text
          </button>
          <button type="button" onClick={askQuestion} disabled={loading || !question || !documentId}>
            Generate Grounded Answer
          </button>
        </div>
        <ApiErrorPanel error={error} />
      </ProductCard>

      <ProductCard
        title="Answer Summary"
        subtitle="Workflow proof is presented before raw model or citation payloads."
        badge={<StatusPill status={answerResponse ? "SUCCESS" : indexResponse?.status ?? "UNKNOWN"} />}
      >
        {answerResponse?.answer ? (
          <div className="answer-box">{answerResponse.answer}</div>
        ) : (
          <EmptyState
            title="No grounded answer yet."
            message="Index text and generate an answer to show controlled AI output with traceability."
          />
        )}
      </ProductCard>

      <ProductCard title="Citation Evidence" subtitle="Citations help reviewers understand which source context supported the answer.">
        {citations.length === 0 ? (
          <EmptyState
            title="No citations yet."
            message="Generate a grounded answer to show the evidence behind the response."
          />
        ) : (
          <div className="citation-list">
            {citations.map((citation, index) => (
              <article key={citation.chunkId ?? index}>
                <div className="citation-heading">
                  <strong>{citation.source ?? "Unknown source"}</strong>
                  <span>Score {citation.score == null ? "-" : citation.score.toFixed(3)}</span>
                </div>
                <p>{citation.excerpt ?? "No excerpt provided."}</p>
              </article>
            ))}
          </div>
        )}
      </ProductCard>

      <ProductCard title="Grounding Details" subtitle="Business context carried with the grounded knowledge request.">
        <div className="summary-grid">
          <div>
            <span>Document ID</span>
            <strong>{documentId}</strong>
          </div>
          <div>
            <span>Tenant ID</span>
            <strong>{demoIdentity.tenantId}</strong>
          </div>
          <div>
            <span>Correlation ID</span>
            <strong>{demoIdentity.correlationId}</strong>
          </div>
        </div>
        <TechnicalDetails data={answerResponse?.citationPackage ?? null} title="Citation package" />
      </ProductCard>

      <ProductCard title="Technical Details" subtitle="Raw API responses for engineering review.">
        <div className="content-grid two-column">
          <div>
            <h4>Index</h4>
            <JsonViewer data={indexResponse} title="Index response" />
          </div>
          <div>
            <h4>Answer</h4>
            <JsonViewer data={answerResponse} title="Answer response" />
          </div>
        </div>
      </ProductCard>
    </section>
  );
}
