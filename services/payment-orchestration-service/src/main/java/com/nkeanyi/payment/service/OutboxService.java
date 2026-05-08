package com.nkeanyi.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.payment.entity.OutboxEvent;
import com.nkeanyi.payment.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveEvent(String aggregateType, String aggregateId, String topic, Object payload) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setTopic(topic);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus("NEW");
            event.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
