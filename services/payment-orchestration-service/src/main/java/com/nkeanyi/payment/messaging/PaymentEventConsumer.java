package com.nkeanyi.payment.messaging;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.repository.PaymentRepository;
import com.nkeanyi.payment.service.PaymentOrchestrationService;
import lombok.extern.slf4j.Slf4j;
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
    public void handlePaymentReceived(PaymentEvent event) {
        if ("FORCE-DLT".equals(event.getPaymentReference())) {
            throw new RuntimeException("Forced consumer failure for DLT integration test");
        }
        Payment payment = paymentRepository.findByPaymentId(event.getPaymentId())
                .orElseThrow(() -> new IllegalStateException("Payment not found for paymentId: " + event.getPaymentId()));

        CreatePaymentRequest request = toRequest(event);
        Payment updatedPayment = paymentOrchestrationService.orchestrate(payment, request);
        if (updatedPayment.getStatus() == PaymentStatus.REJECTED
                || updatedPayment.getStatus() == PaymentStatus.FAILED) {
            paymentEventPublisher.publish(KafkaTopics.PAYMENT_REJECTED, event);
            return;
        }

        paymentEventPublisher.publish(KafkaTopics.PAYMENT_PROCESSED, event);
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
