package com.nkeanyi.payment.messaging;

import com.nkeanyi.payment.event.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, PaymentEvent event) {
        kafkaTemplate.send(topic, event.getPaymentId(), event);
        log.info("Published event topic={} paymentId={}", topic, event.getPaymentId());
    }
}
