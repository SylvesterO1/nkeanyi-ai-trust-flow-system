package com.nkeanyi.knowledge.observability;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

public final class KafkaMdcContext {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_IDEMPOTENCY_KEY = "idempotencyKey";

    private static final String UNKNOWN_TENANT = "unknown-tenant";
    private static final String UNKNOWN_CORRELATION = "unknown-correlation";
    private static final String UNKNOWN_IDEMPOTENCY_KEY = "unknown-idempotency-key";

    private KafkaMdcContext() {
    }

    public static void runWithKafkaHeaders(Headers headers, Runnable action) {
        String tenantId = readHeader(headers, TENANT_HEADER, UNKNOWN_TENANT);
        String correlationId = readHeader(headers, CORRELATION_HEADER, UNKNOWN_CORRELATION);
        String idempotencyKey = readHeader(headers, IDEMPOTENCY_HEADER, UNKNOWN_IDEMPOTENCY_KEY);

        MDC.put(MDC_TENANT_ID, tenantId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_IDEMPOTENCY_KEY, idempotencyKey);

        try {
            action.run();
        } finally {
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_IDEMPOTENCY_KEY);
        }
    }

    private static String readHeader(Headers headers, String headerName, String fallback) {
        if (headers == null) {
            return fallback;
        }

        Header header = headers.lastHeader(headerName);

        if (header == null || header.value() == null || header.value().length == 0) {
            return fallback;
        }

        String value = new String(header.value(), StandardCharsets.UTF_8);

        if (value.isBlank()) {
            return fallback;
        }

        return value;
    }
}
