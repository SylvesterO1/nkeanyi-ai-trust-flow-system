package com.nkeanyi.payment.observability;

import org.slf4j.MDC;

public final class MdcContextReader {

    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_IDEMPOTENCY_KEY = "idempotencyKey";

    private static final String UNKNOWN_TENANT = "unknown-tenant";
    private static final String UNKNOWN_CORRELATION = "unknown-correlation";
    private static final String UNKNOWN_IDEMPOTENCY_KEY = "unknown-idempotency-key";

    private MdcContextReader() {
    }

    public static String tenantId() {
        String tenantId = MDC.get(MDC_TENANT_ID);
        return tenantId == null || tenantId.isBlank() ? UNKNOWN_TENANT : tenantId;
    }

    public static String correlationId() {
        String correlationId = MDC.get(MDC_CORRELATION_ID);
        return correlationId == null || correlationId.isBlank() ? UNKNOWN_CORRELATION : correlationId;
    }

    public static String idempotencyKey() {
        String idempotencyKey = MDC.get(MDC_IDEMPOTENCY_KEY);
        return idempotencyKey == null || idempotencyKey.isBlank() ? UNKNOWN_IDEMPOTENCY_KEY : idempotencyKey;
    }
}
