package com.nkeanyi.compliance.kafka;

import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.service.ComplianceEvidenceService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ComplianceEvidenceKafkaConsumer {

    private final ComplianceEvidenceService service;

    public ComplianceEvidenceKafkaConsumer(ComplianceEvidenceService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.document-extracted}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeDocumentExtracted(ConsumerRecord<String, String> record) {
        service.createEvidenceFromKafka(
                record.topic(),
                record.key(),
                EvidenceType.DOCUMENT_EXTRACTED,
                "document-intelligence-service",
                record.value()
        );
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.knowledge-answer-generated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeKnowledgeAnswerGenerated(ConsumerRecord<String, String> record) {
        service.createEvidenceFromKafka(
                record.topic(),
                record.key(),
                EvidenceType.KNOWLEDGE_ANSWER_GENERATED,
                "knowledge-service",
                record.value()
        );
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.payment-audit}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentAudit(ConsumerRecord<String, String> record) {
        service.createEvidenceFromKafka(
                record.topic(),
                record.key(),
                EvidenceType.PAYMENT_AUDIT,
                "payment-orchestration-service",
                record.value()
        );
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.payment-status-changed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentStatusChanged(ConsumerRecord<String, String> record) {
        service.createEvidenceFromKafka(
                record.topic(),
                record.key(),
                EvidenceType.PAYMENT_STATUS_CHANGED,
                "payment-orchestration-service",
                record.value()
        );
    }
}
