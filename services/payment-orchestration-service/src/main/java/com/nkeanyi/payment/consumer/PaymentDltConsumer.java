package com.nkeanyi.payment.consumer;

import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.messaging.KafkaTopics;
import com.nkeanyi.payment.repository.PaymentRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PaymentDltConsumer {

    private final PaymentRepository paymentRepository;

    public PaymentDltConsumer(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_RECEIVED_DLT, groupId = "payment-dlt-group")
    @Transactional
    public void handleDlt(PaymentEvent event) {
        Payment payment = paymentRepository.findByPaymentId(event.getPaymentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found for DLT event: " + event.getPaymentId()
                ));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setDecisionReason("Moved to DLT after consumer failure");
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
}
