package com.nkeanyi.documentintelligence.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentExtractedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public DocumentExtractedPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${DOCUMENT_EXTRACTED_TOPIC:document.extracted.v1}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(DocumentExtractedEvent event) {
        kafkaTemplate.send(topic, event.objectName(), event);
    }
}
