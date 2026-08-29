import { apiPost } from "./client";
import type { CreatePaymentRequest, PaymentResponse, RequestOptions } from "../types/api";

export function createPayment(request: CreatePaymentRequest, options?: RequestOptions) {
  return apiPost<PaymentResponse>("/api/v1/payments", request, {
    ...options,
    tenantId: options?.tenantId ?? request.tenantId,
    correlationId: options?.correlationId ?? request.correlationId,
    idempotencyKey: options?.idempotencyKey ?? request.idempotencyKey,
  });
}
