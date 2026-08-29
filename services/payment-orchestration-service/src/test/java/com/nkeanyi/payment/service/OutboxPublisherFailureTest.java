package com.nkeanyi.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.payment.entity.OutboxEvent;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.observability.PaymentMetricsService;
import com.nkeanyi.payment.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPublisherFailureTest {

    @Test
    void kafkaSendFailureMarksOutboxEventFailedAndCanBeRetriedWhenResetToNew() {
        OutboxEvent event = event("NEW");
        CapturingRepository repository = new CapturingRepository(event);
        FailingOnceOutboxPublisher publisher = new FailingOnceOutboxPublisher(repository.proxy());

        publisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(repository.savedStatuses).contains("FAILED");

        event.setStatus("NEW");
        publisher.fail = false;
        publisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(repository.savedStatuses).contains("FAILED", "PUBLISHED");
    }

    private OutboxEvent event(String status) {
        OutboxEvent event = new OutboxEvent();
        event.setEventId("event-1");
        event.setAggregateType("PAYMENT");
        event.setAggregateId("pay-1");
        event.setTopic("payment.received");
        event.setStatus(status);
        event.setTenantId("tenant-a");
        event.setCorrelationId("corr-a");
        event.setCreatedAt(LocalDateTime.now());
        event.setPayload("""
                {
                  "paymentId": "pay-1",
                  "customerId": "cust-1",
                  "idempotencyKey": "idem-1",
                  "amount": 100.00,
                  "currency": "USD"
                }
                """);
        return event;
    }

    private static class FailingOnceOutboxPublisher extends OutboxPublisher {
        private boolean fail = true;

        FailingOnceOutboxPublisher(OutboxEventRepository repository) {
            super(repository, null, new ObjectMapper(), null);
        }

        @Override
        protected void sendRecord(ProducerRecord<String, PaymentEvent> record) {
            if (fail) {
                throw new IllegalStateException("kafka send failed");
            }
        }
    }

    private static class CapturingRepository {
        private final OutboxEvent event;
        private final List<String> savedStatuses = new ArrayList<>();

        CapturingRepository(OutboxEvent event) {
            this.event = event;
        }

        OutboxEventRepository proxy() {
            return (OutboxEventRepository) Proxy.newProxyInstance(
                    OutboxEventRepository.class.getClassLoader(),
                    new Class<?>[] {OutboxEventRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findTop50ByStatusOrderByCreatedAtAsc" -> "NEW".equals(event.getStatus()) ? List.of(event) : List.of();
                        case "save" -> {
                            OutboxEvent saved = (OutboxEvent) args[0];
                            savedStatuses.add(saved.getStatus());
                            yield saved;
                        }
                        case "toString" -> "OutboxEventRepository test proxy";
                        default -> throw new UnsupportedOperationException(method.getName() + " is not used in this test");
                    }
            );
        }
    }
}
