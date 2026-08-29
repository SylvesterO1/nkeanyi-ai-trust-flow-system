package com.nkeanyi.knowledge.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeAnswerGeneratedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KnowledgeAnswerGeneratedPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${KNOWLEDGE_ANSWER_GENERATED_TOPIC:knowledge.answer.generated.v1}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(KnowledgeAnswerGeneratedEvent event) {
        kafkaTemplate.send(topic, event.documentId(), event);
    }
}
