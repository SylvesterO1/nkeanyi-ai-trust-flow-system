import { apiGet, apiPost } from "./client";
import type {
  KnowledgeAnswerRequest,
  KnowledgeAnswerResponse,
  KnowledgeEvaluationResponse,
  KnowledgeFeedbackRequest,
  KnowledgeFeedbackResponse,
  KnowledgeIndexRequest,
  KnowledgeIndexResponse,
  KnowledgeOperationsMetricsResponse,
  RequestOptions,
  RetrievalEventsResponse,
} from "../types/api";

export function indexKnowledgeText(request: KnowledgeIndexRequest, options?: RequestOptions) {
  return apiPost<KnowledgeIndexResponse>("/api/v1/knowledge/index-text", request, options);
}

export function generateKnowledgeAnswer(request: KnowledgeAnswerRequest, options?: RequestOptions) {
  return apiPost<KnowledgeAnswerResponse>("/api/v1/knowledge/answer", request, options);
}

export function recordKnowledgeFeedback(request: KnowledgeFeedbackRequest, options?: RequestOptions) {
  return apiPost<KnowledgeFeedbackResponse>("/api/v1/knowledge/feedback", request, options);
}

export function listKnowledgeRetrievalEvents(
  options?: RequestOptions & { limit?: number; status?: string },
) {
  const searchParams = new URLSearchParams();
  if (options?.tenantId) searchParams.set("tenantId", options.tenantId);
  if (options?.status) searchParams.set("status", options.status);
  if (options?.limit) searchParams.set("limit", String(options.limit));
  const query = searchParams.toString();
  return apiGet<RetrievalEventsResponse>(
    `/api/v1/knowledge/retrieval-events${query ? `?${query}` : ""}`,
    options,
  );
}

export function runKnowledgeEvaluation(options?: RequestOptions) {
  return apiPost<KnowledgeEvaluationResponse>("/api/v1/knowledge/evaluation/run", {}, options);
}

export function getKnowledgeMetrics(options?: RequestOptions) {
  return apiGet<KnowledgeOperationsMetricsResponse>("/api/v1/knowledge/operations/metrics", options);
}
