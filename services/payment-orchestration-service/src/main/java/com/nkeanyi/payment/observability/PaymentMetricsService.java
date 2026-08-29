package com.nkeanyi.payment.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentMetricsService {

    private final Counter paymentCreatedCounter;
    private final Counter paymentReplayCounter;
    private final Counter outboxPublishedCounter;
    private final Counter outboxPublishFailedCounter;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> statusCounters = new ConcurrentHashMap<>();

    public PaymentMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.paymentCreatedCounter = Counter.builder("payment.created.count")
                .description("Number of payments created")
                .register(meterRegistry);

        this.paymentReplayCounter = Counter.builder("payment.idempotency.replay.count")
                .description("Number of idempotent replay requests")
                .register(meterRegistry);

        this.outboxPublishedCounter = Counter.builder("outbox.published.count")
                .description("Number of outbox events successfully published")
                .register(meterRegistry);

        this.outboxPublishFailedCounter = Counter.builder("outbox.publish.failed.count")
                .description("Number of outbox events that failed publishing")
                .register(meterRegistry);

        preRegisterStatus("RECEIVED");
        preRegisterStatus("VALIDATING");
        preRegisterStatus("COMPLIANCE_REVIEW");
        preRegisterStatus("ANOMALY_REVIEW");
        preRegisterStatus("FRAUD_REVIEW");
        preRegisterStatus("APPROVED");
        preRegisterStatus("PROCESSING");
        preRegisterStatus("COMPLETED");
        preRegisterStatus("REJECTED");
        preRegisterStatus("FAILED");
    }

    public void incrementPaymentCreated() {
        paymentCreatedCounter.increment();
    }

    public void incrementPaymentReplay() {
        paymentReplayCounter.increment();
    }

    public void incrementPaymentStatusTransition(String status) {
        statusCounters.computeIfAbsent(status, this::buildStatusCounter).increment();
    }

    public void incrementOutboxPublished() {
        outboxPublishedCounter.increment();
    }

    public void incrementOutboxPublishFailed() {
        outboxPublishFailedCounter.increment();
    }

    private void preRegisterStatus(String status) {
        statusCounters.put(status, buildStatusCounter(status));
    }

    private Counter buildStatusCounter(String status) {
        return Counter.builder("payment.status.transition.count")
                .description("Number of payment status transitions")
                .tag("status", status)
                .register(meterRegistry);
    }
}
