package com.nkeanyi.payment.messaging;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.observability.KafkaMdcContext;
import com.nkeanyi.payment.repository.PaymentRepository;
import com.nkeanyi.payment.service.PaymentOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventConsumer {

    private final PaymentRepository paymentRepository;
    private final PaymentOrchestrationService paymentOrchestrationService;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentEventConsumer(PaymentRepository paymentRepository,
                                PaymentOrchestrationService paymentOrchestrationService,
                                PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentOrchestrationService = paymentOrchestrationService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_RECEIVED, groupId = "payment-orchestration-group")
    public void handlePaymentReceived(ConsumerRecord<String, PaymentEvent> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            PaymentEvent event = record.value();

            if (event == null) {
                log.warn(
                        "payment_event_consumer_received_null_event topic={} partition={} offset={} key={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key()
                );
                return;
            }

            log.info(
                    "payment_event_received topic={} partition={} offset={} key={} paymentId={} paymentReference={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    event.getPaymentId(),
                    event.getPaymentReference()
            );

            if ("FORCE-DLT".equals(event.getPaymentReference())) {
                throw new RuntimeException("Forced consumer failure for DLT integration test");
            }

            Payment payment = paymentRepository.findByPaymentId(event.getPaymentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Payment not found for paymentId: " + event.getPaymentId()
                    ));

            CreatePaymentRequest request = toRequest(event);
            Payment updatedPayment = paymentOrchestrationService.orchestrate(payment, request);

            if (updatedPayment.getStatus() == PaymentStatus.REJECTED
                    || updatedPayment.getStatus() == PaymentStatus.FAILED) {
                applyFinalAuditDecision(event, updatedPayment);

                log.info(
                        "payment_event_routed topic={} paymentId={} status={}",
                        KafkaTopics.PAYMENT_REJECTED,
                        updatedPayment.getPaymentId(),
                        updatedPayment.getStatus()
                );

                paymentEventPublisher.publish(KafkaTopics.PAYMENT_REJECTED, event);
                return;
            }

            applyFinalAuditDecision(event, updatedPayment);

            log.info(
                    "payment_event_routed topic={} paymentId={} status={}",
                    KafkaTopics.PAYMENT_PROCESSED,
                    updatedPayment.getPaymentId(),
                    updatedPayment.getStatus()
            );

            paymentEventPublisher.publish(KafkaTopics.PAYMENT_PROCESSED, event);
        });
    }

    private void applyFinalAuditDecision(PaymentEvent event, Payment payment) {
        event.setDecision(payment.getStatus().name());

        Double riskScore = payment.getRiskScore() == null
                ? null
                : payment.getRiskScore().doubleValue();

        event.setRiskScore(riskScore);
        event.setRiskLevel(resolveRiskLevel(riskScore, payment.getStatus()));
    }

    private String resolveRiskLevel(Double riskScore, PaymentStatus status) {
        if (status == PaymentStatus.REJECTED || status == PaymentStatus.FAILED) {
            return "HIGH";
        }

        if (riskScore == null) {
            return "UNKNOWN";
        }

        if (riskScore >= 80.0) {
            return "HIGH";
        }

        if (riskScore >= 50.0) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private CreatePaymentRequest toRequest(PaymentEvent event) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCustomerId(event.getCustomerId());
        request.setSourceAccount(event.getSourceAccount());
        request.setDestinationAccount(event.getDestinationAccount());
        request.setAmount(event.getAmount());
        request.setCurrency(event.getCurrency());
        request.setPaymentReference(event.getPaymentReference());
        request.setPaymentMethod(event.getPaymentMethod());
        request.setNarration(event.getNarration());
        return request;
    }
}
