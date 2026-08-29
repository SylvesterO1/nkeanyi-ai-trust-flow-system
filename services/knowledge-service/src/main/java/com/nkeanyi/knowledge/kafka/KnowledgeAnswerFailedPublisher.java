package com.nkeanyi.knowledge.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeAnswerFailedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KnowledgeAnswerFailedPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${KNOWLEDGE_ANSWER_FAILED_TOPIC:knowledge.answer.failed.v1}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(KnowledgeAnswerFailedEvent event) {
        kafkaTemplate.send(topic, event.documentId(), event);
    }
}
