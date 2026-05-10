package com.nkeanyi.knowledge.observability;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaTenantCorrelationProducerInterceptor implements ProducerInterceptor<String, String> {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        String tenantId = MDC.get(MDC_TENANT_ID);
        String correlationId = MDC.get(MDC_CORRELATION_ID);

        Headers headers = record.headers();

        if (tenantId != null && !tenantId.isBlank()) {
            headers.remove(TENANT_HEADER);
            headers.add(TENANT_HEADER, tenantId.getBytes(StandardCharsets.UTF_8));
        }

        if (correlationId != null && !correlationId.isBlank()) {
            headers.remove(CORRELATION_HEADER);
            headers.add(CORRELATION_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // No-op. Logging remains inside service/business flow to avoid noisy Kafka internals.
    }

    @Override
    public void close() {
        // No-op.
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // No-op.
    }
}
