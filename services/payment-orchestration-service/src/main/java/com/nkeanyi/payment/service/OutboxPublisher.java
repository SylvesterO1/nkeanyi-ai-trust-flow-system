package com.nkeanyi.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.payment.entity.OutboxEvent;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.observability.PaymentMetricsService;
import com.nkeanyi.payment.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OutboxPublisher {

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
                kafkaTemplate.send(event.getTopic(), payload.getPaymentId(), payload);
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                paymentMetricsService.incrementOutboxPublished();
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={}", event.getId(), ex);
                event.setStatus("FAILED");
                outboxEventRepository.save(event);
                paymentMetricsService.incrementOutboxPublishFailed();
            }
        }
    }
}
