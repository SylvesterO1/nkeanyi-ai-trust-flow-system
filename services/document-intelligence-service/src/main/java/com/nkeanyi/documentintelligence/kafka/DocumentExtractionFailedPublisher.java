package com.nkeanyi.documentintelligence.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentExtractionFailedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public DocumentExtractionFailedPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${DOCUMENT_EXTRACTION_FAILED_TOPIC:document.extraction.failed.v1}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(DocumentExtractionFailedEvent event) {
        kafkaTemplate.send(topic, event.idempotencyKey(), event);
    }
}
