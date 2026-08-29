package com.nkeanyi.documentintelligence.observability;

import org.slf4j.MDC;

public final class RegulatedRequestContext {

    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_IDEMPOTENCY_KEY = "idempotencyKey";

    private RegulatedRequestContext() {
    }

    public static String tenantId() {
        return read(MDC_TENANT_ID, "unknown-tenant");
    }

    public static String correlationId() {
        return read(MDC_CORRELATION_ID, "unknown-correlation");
    }

    public static String idempotencyKey() {
        return read(MDC_IDEMPOTENCY_KEY, "unknown-idempotency-key");
    }

    private static String read(String key, String fallback) {
        String value = MDC.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
