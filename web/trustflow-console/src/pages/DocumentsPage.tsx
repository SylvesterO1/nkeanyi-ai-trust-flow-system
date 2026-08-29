import { useState } from "react";
import {
  getDocumentRecord,
  getDocumentRecordsByCorrelationId,
  listDocumentRecords,
  processDocument,
} from "../api/documentsApi";
import { ApiErrorPanel } from "../components/ApiErrorPanel";
import { AuthNotice } from "../components/AuthNotice";
import { DemoAuthPanel } from "../components/DemoAuthPanel";
import { DemoIdentityPanel } from "../components/DemoIdentityPanel";
import { DocumentRecordsTable } from "../components/DocumentRecordsTable";
import { JsonViewer } from "../components/JsonViewer";
import { EmptyState, PageHeader, ProductCard, StatusPill, TechnicalDetails } from "../components/ProductUI";
import { WhatThisProves } from "../components/WhatThisProves";
import { useDemoIdentity, useDemoToken } from "../utils/demoStorage";
import type { DocumentProcessResponse, DocumentRecord, RequestOptions } from "../types/api";

function documentIdFrom(response: DocumentProcessResponse | null) {
  return response?.documentId ?? response?.objectName ?? "Pending document processing";
}

export function DocumentsPage() {
  const demoIdentity = useDemoIdentity();
  const { token } = useDemoToken();
  const [file, setFile] = useState<File | null>(null);
  const [response, setResponse] = useState<DocumentProcessResponse | null>(null);
  const [records, setRecords] = useState<DocumentRecord[]>([]);
  const [selectedRecord, setSelectedRecord] = useState<DocumentRecord | null>(null);
  const [documentIdSearch, setDocumentIdSearch] = useState("");
  const [correlationIdSearch, setCorrelationIdSearch] = useState(demoIdentity.correlationId);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);
  const [recordsLoading, setRecordsLoading] = useState(false);

  const requestOptions: RequestOptions = {
    tenantId: demoIdentity.tenantId,
    correlationId: demoIdentity.correlationId,
    idempotencyKey: demoIdentity.idempotencyKey,
    authorizationToken: token || undefined,
  };

  async function submitDocument() {
    if (!file) {
      setError(new Error("Choose a document file before processing."));
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const result = await processDocument(file, requestOptions);
      setResponse(result);
      setDocumentIdSearch(result.documentId ?? result.objectName ?? "");
      await loadRecentRecords();
    } catch (err) {
      setResponse(null);
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  async function loadRecentRecords() {
    setError(null);
    setRecordsLoading(true);

    try {
      const result = await listDocumentRecords(requestOptions);
      setRecords(result.records);
    } catch (err) {
      setRecords([]);
      setError(err);
    } finally {
      setRecordsLoading(false);
    }
  }

  async function searchByDocumentId() {
    if (!documentIdSearch) {
      setError(new Error("Enter a documentId before searching."));
      return;
    }

    setError(null);
    setRecordsLoading(true);

    try {
      const record = await getDocumentRecord(documentIdSearch, requestOptions);
      setRecords([record]);
      setSelectedRecord(record);
    } catch (err) {
      setRecords([]);
      setSelectedRecord(null);
      setError(err);
    } finally {
      setRecordsLoading(false);
    }
  }

  async function searchByCorrelationId() {
    if (!correlationIdSearch) {
      setError(new Error("Enter a correlationId before searching."));
      return;
    }

    setError(null);
    setRecordsLoading(true);

    try {
      const result = await getDocumentRecordsByCorrelationId(correlationIdSearch, requestOptions);
      setRecords(result.records);
      setSelectedRecord(null);
    } catch (err) {
      setRecords([]);
      setSelectedRecord(null);
      setError(err);
    } finally {
      setRecordsLoading(false);
    }
  }

  async function viewDetails(documentId: string) {
    setError(null);

    try {
      setSelectedRecord(await getDocumentRecord(documentId, requestOptions));
    } catch (err) {
      setSelectedRecord(null);
      setError(err);
    }
  }

  return (
    <section className="page-stack">
      <PageHeader
        title="Document Intelligence Control"
        subtitle="Controlled extraction with traceable document IDs, tenant context, and correlation IDs."
      />

      <DemoAuthPanel />
      <AuthNotice />
      <DemoIdentityPanel onIdentityChange={demoIdentity.setDemoIdentity} />

      <WhatThisProves
        title="What This Proves"
        points={[
          "Documents are processed with extraction metadata",
          "Each document can be tied to tenant and correlation context",
          "Extraction results can be audited",
        ]}
      />

      <ProductCard
        title="Process Document"
        subtitle="Upload a business document and create a tenant-aware extraction record."
      >
        <div className="form-grid">
          <label className="wide-field">
            Document File
            <input type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
          </label>
        </div>
        <div className="button-row">
          <button type="button" onClick={submitDocument} disabled={loading}>
            Process Document
          </button>
        </div>
        <ApiErrorPanel error={error} />
      </ProductCard>

      <ProductCard
        title="Extraction Result Summary"
        subtitle="Human-readable proof before inspecting the API payload."
        badge={<StatusPill status={response?.status ?? response?.eventStatus ?? "UNKNOWN"} />}
      >
        <div className="summary-grid">
          <div>
            <span>Document ID</span>
            <strong>{documentIdFrom(response)}</strong>
          </div>
          <div>
            <span>Extractor Used</span>
            <strong>{response?.extractorUsed ?? "-"}</strong>
          </div>
          <div>
            <span>Confidence</span>
            <strong>{response?.confidence == null ? "-" : `${Math.round(response.confidence * 100)}%`}</strong>
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
      </ProductCard>

      <ProductCard title="Extracted Text Preview" subtitle="Professional preview of the controlled extraction output.">
        {response?.extractedText ? (
          <div className="text-result">{response.extractedText}</div>
        ) : (
          <EmptyState
            title="No extraction output yet."
            message="Process a document to produce extracted text tied to this tenant and correlation ID."
          />
        )}
      </ProductCard>

      <section className="panel">
        <div className="panel-header">
          <div>
            <h3>Recent Document Records</h3>
            <p>Search document processing records by document or correlation identifiers.</p>
          </div>
          <button type="button" className="secondary-button" onClick={loadRecentRecords} disabled={recordsLoading}>
            Load Recent Records
          </button>
        </div>
        <div className="form-grid">
          <label>
            Document ID
            <input value={documentIdSearch} onChange={(event) => setDocumentIdSearch(event.target.value)} />
          </label>
          <label>
            Correlation ID
            <input value={correlationIdSearch} onChange={(event) => setCorrelationIdSearch(event.target.value)} />
          </label>
        </div>
        <div className="button-row">
          <button type="button" onClick={searchByDocumentId} disabled={recordsLoading || !documentIdSearch}>
            Search by Document ID
          </button>
          <button type="button" onClick={searchByCorrelationId} disabled={recordsLoading || !correlationIdSearch}>
            Search by Correlation ID
          </button>
        </div>
        <DocumentRecordsTable records={records} onSelect={(record) => viewDetails(record.documentId)} />
        {selectedRecord && (
          <div className="record-detail">
            <h4>Document Details</h4>
            <div className="summary-grid">
              <div>
                <span>Document ID</span>
                <strong>{selectedRecord.documentId}</strong>
              </div>
              <div>
                <span>Filename</span>
                <strong>{selectedRecord.originalFilename ?? "-"}</strong>
              </div>
              <div>
                <span>Content Type</span>
                <strong>{selectedRecord.contentType ?? "-"}</strong>
              </div>
              <div>
                <span>Correlation ID</span>
                <strong>{selectedRecord.correlationId ?? "-"}</strong>
              </div>
              <div>
                <span>Tenant ID</span>
                <strong>{selectedRecord.tenantId ?? "-"}</strong>
              </div>
              <div>
                <span>Status</span>
                <strong>{selectedRecord.status ?? "-"}</strong>
              </div>
              <div>
                <span>Extractor</span>
                <strong>{selectedRecord.extractorUsed ?? "-"}</strong>
              </div>
              <div>
                <span>Confidence</span>
                <strong>
                  {selectedRecord.confidence == null ? "-" : `${Math.round(selectedRecord.confidence * 100)}%`}
                </strong>
              </div>
              <div>
                <span>Updated At</span>
                <strong>{selectedRecord.updatedAt ?? "-"}</strong>
              </div>
            </div>
            <div className="text-result">
              {selectedRecord.extractedText || selectedRecord.errorMessage || "No extracted text available."}
            </div>
            <JsonViewer data={selectedRecord} />
          </div>
        )}
      </section>

      <ProductCard title="Evidence and Technical Details" subtitle="Raw API response remains available for engineering review.">
        <TechnicalDetails data={response} />
      </ProductCard>
    </section>
  );
}
