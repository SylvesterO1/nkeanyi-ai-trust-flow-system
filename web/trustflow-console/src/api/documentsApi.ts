import { apiGet, apiPost, apiPostForm } from "./client";
import type {
  DocumentAuditEventsResponse,
  DocumentFeedbackRequest,
  DocumentFeedbackResponse,
  DocumentOperationsMetricsResponse,
  DocumentProcessResponse,
  DocumentRecord,
  DocumentRecordsResponse,
  DocumentReviewRequest,
  DocumentReviewTasksResponse,
  DocumentWorkflow,
  RequestOptions,
} from "../types/api";

export function processDocument(file: File, options?: RequestOptions) {
  const body = new FormData();
  body.append("file", file);

  return apiPostForm<DocumentProcessResponse>("/api/v1/documents/process", body, options);
}

type DocumentRecordQueryOptions = RequestOptions & {
  limit?: number;
};

function withQuery(path: string, params: Record<string, string | number | undefined>) {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value != null && value !== "") {
      searchParams.set(key, String(value));
    }
  });

  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
}

export function getRecentDocumentRecords(options?: DocumentRecordQueryOptions) {
  return apiGet<DocumentRecordsResponse>(
    withQuery("/api/v1/documents", {
      correlationId: options?.correlationId,
      limit: options?.limit,
      tenantId: options?.tenantId,
    }),
    options,
  );
}

export function getDocumentById(documentId: string, options?: RequestOptions) {
  return apiGet<DocumentRecord>(`/api/v1/documents/${encodeURIComponent(documentId)}`, options);
}

export function getDocumentsByCorrelationId(
  correlationId: string,
  options?: DocumentRecordQueryOptions,
) {
  return apiGet<DocumentRecordsResponse>(
    withQuery(`/api/v1/documents/correlation/${encodeURIComponent(correlationId)}`, {
      limit: options?.limit,
      tenantId: options?.tenantId,
    }),
    { ...options, correlationId },
  );
}

export const listDocumentRecords = getRecentDocumentRecords;
export const getDocumentRecord = getDocumentById;
export const getDocumentRecordsByCorrelationId = getDocumentsByCorrelationId;

export function getDocumentWorkflow(documentId: string, options?: RequestOptions) {
  return apiGet<DocumentWorkflow>(
    `/api/v1/documents/enterprise/${encodeURIComponent(documentId)}/workflow`,
    options,
  );
}

export function listDocumentReviewTasks(
  options?: DocumentRecordQueryOptions & { status?: string; riskLevel?: string },
) {
  return apiGet<DocumentReviewTasksResponse>(
    withQuery("/api/v1/documents/enterprise/reviews", {
      limit: options?.limit,
      riskLevel: options?.riskLevel,
      status: options?.status,
      tenantId: options?.tenantId,
    }),
    options,
  );
}

export function reviewDocument(
  documentId: string,
  request: DocumentReviewRequest,
  options?: RequestOptions,
) {
  return apiPost<DocumentWorkflow>(
    `/api/v1/documents/enterprise/${encodeURIComponent(documentId)}/reviews`,
    request,
    options,
  );
}

export function publishDocumentWorkflow(
  documentId: string,
  actorId?: string,
  options?: RequestOptions,
) {
  return apiPost<DocumentWorkflow>(
    withQuery(`/api/v1/documents/enterprise/${encodeURIComponent(documentId)}/publish`, {
      actorId,
    }),
    {},
    options,
  );
}

export function recordDocumentFeedback(
  documentId: string,
  request: DocumentFeedbackRequest,
  options?: RequestOptions,
) {
  return apiPost<DocumentFeedbackResponse>(
    `/api/v1/documents/enterprise/${encodeURIComponent(documentId)}/feedback`,
    request,
    options,
  );
}

export function searchDocumentAudit(
  options?: DocumentRecordQueryOptions & {
    action?: string;
    documentId?: string;
    from?: string;
    to?: string;
    userId?: string;
  },
) {
  return apiGet<DocumentAuditEventsResponse>(
    withQuery("/api/v1/documents/enterprise/audit", {
      action: options?.action,
      documentId: options?.documentId,
      from: options?.from,
      limit: options?.limit,
      tenantId: options?.tenantId,
      to: options?.to,
      userId: options?.userId,
    }),
    options,
  );
}

export function getDocumentEnterpriseMetrics(options?: RequestOptions) {
  return apiGet<DocumentOperationsMetricsResponse>("/api/v1/documents/enterprise/metrics", options);
}
