import { apiGet } from "./client";
import type { EvidenceSearchResponse, RequestOptions } from "../types/api";

export function getEvidenceByCorrelationId(
  correlationId: string,
  tenantIdOrOptions?: string | RequestOptions,
) {
  const options =
    typeof tenantIdOrOptions === "string" ? { tenantId: tenantIdOrOptions } : tenantIdOrOptions;

  return apiGet<EvidenceSearchResponse>(
    `/api/v1/evidence/correlation/${encodeURIComponent(correlationId)}`,
    { ...options, correlationId },
  );
}

export function getEvidenceByPaymentId(paymentId: string, tenantIdOrOptions?: string | RequestOptions) {
  const options =
    typeof tenantIdOrOptions === "string" ? { tenantId: tenantIdOrOptions } : tenantIdOrOptions;

  return apiGet<EvidenceSearchResponse>(
    `/api/v1/evidence/payment/${encodeURIComponent(paymentId)}`,
    options,
  );
}
