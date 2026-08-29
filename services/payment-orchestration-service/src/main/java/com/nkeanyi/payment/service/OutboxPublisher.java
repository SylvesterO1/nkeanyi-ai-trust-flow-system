package com.nkeanyi.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.payment.entity.OutboxEvent;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.observability.PaymentMetricsService;
import com.nkeanyi.payment.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OutboxPublisher {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private static final String UNKNOWN_TENANT = "unknown-tenant";
    private static final String UNKNOWN_CORRELATION = "unknown-correlation";
    private static final String UNKNOWN_IDEMPOTENCY_KEY = "unknown-idempotency-key";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentMetricsService paymentMetricsService;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, PaymentEvent> kafkaTemplate,
                           ObjectMapper objectMapper,
                           PaymentMetricsService paymentMetricsService) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.paymentMetricsService = paymentMetricsService;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("NEW");

        for (OutboxEvent event : events) {
            try {
                PaymentEvent payload = objectMapper.readValue(event.getPayload(), PaymentEvent.class);

                ProducerRecord<String, PaymentEvent> record = new ProducerRecord<>(
                        event.getTopic(),
                        payload.getPaymentId(),
                        payload
                );

                record.headers().add(
                        TENANT_HEADER,
                        safeHeader(event.getTenantId(), UNKNOWN_TENANT).getBytes(StandardCharsets.UTF_8)
                );

                record.headers().add(
                        CORRELATION_HEADER,
                        safeHeader(event.getCorrelationId(), UNKNOWN_CORRELATION).getBytes(StandardCharsets.UTF_8)
                );

                record.headers().add(
                        IDEMPOTENCY_HEADER,
                        safeHeader(payload.getIdempotencyKey(), UNKNOWN_IDEMPOTENCY_KEY).getBytes(StandardCharsets.UTF_8)
                );

                sendRecord(record);

                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                if (paymentMetricsService != null) {
                    paymentMetricsService.incrementOutboxPublished();
                }

                log.info(
                        "outbox_event_published id={} eventId={} topic={} aggregateId={} tenantId={} correlationId={}",
                        event.getId(),
                        event.getEventId(),
                        event.getTopic(),
                        event.getAggregateId(),
                        safeHeader(event.getTenantId(), UNKNOWN_TENANT),
                        safeHeader(event.getCorrelationId(), UNKNOWN_CORRELATION)
                );
            } catch (Exception ex) {
                log.error("failed_to_publish_outbox_event id={} eventId={} topic={}",
                        event.getId(),
                        event.getEventId(),
                        event.getTopic(),
                        ex
                );

                event.setStatus("FAILED");
                outboxEventRepository.save(event);
                if (paymentMetricsService != null) {
                    paymentMetricsService.incrementOutboxPublishFailed();
                }
            }
        }
    }

    protected void sendRecord(ProducerRecord<String, PaymentEvent> record) {
        kafkaTemplate.send(record);
    }

    private String safeHeader(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
