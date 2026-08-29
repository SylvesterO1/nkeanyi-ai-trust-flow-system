package com.nkeanyi.compliance.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.observability.KafkaMdcContext;
import com.nkeanyi.compliance.service.ComplianceEvidenceService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ComplianceEvidenceKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ComplianceEvidenceKafkaConsumer.class);

    private final ComplianceEvidenceService service;
    private final ObjectMapper objectMapper;

    public ComplianceEvidenceKafkaConsumer(ComplianceEvidenceService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.document-extracted}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeDocumentExtracted(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            log.info(
                    "compliance_evidence_event_received type={} topic={} partition={} offset={} key={}",
                    EvidenceType.DOCUMENT_EXTRACTED,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );

            service.createEvidenceFromKafka(
                    record.topic(),
                    record.key(),
                    EvidenceType.DOCUMENT_EXTRACTED,
                    "document-intelligence-service",
                    record.value(),
                    KafkaMdcContext.tenantId(),
                    KafkaMdcContext.correlationId()
            );
        });
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.document-extraction-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeDocumentExtractionFailed(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> service.createEvidenceFromKafka(
                record.topic(),
                record.key(),
                EvidenceType.DOCUMENT_EXTRACTION_FAILED,
                "document-intelligence-service",
                record.value(),
                KafkaMdcContext.tenantId(),
                KafkaMdcContext.correlationId()
        ));
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.knowledge-answer-generated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeKnowledgeAnswerGenerated(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            log.info(
                    "compliance_evidence_event_received type={} topic={} partition={} offset={} key={}",
                    EvidenceType.KNOWLEDGE_ANSWER_GENERATED,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );

            service.createEvidenceFromKafka(
                    record.topic(),
                    record.key(),
                    EvidenceType.KNOWLEDGE_ANSWER_GENERATED,
                    "knowledge-service",
                    record.value(),
                    KafkaMdcContext.tenantId(),
                    KafkaMdcContext.correlationId()
            );
        });
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.knowledge-answer-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeKnowledgeAnswerFailed(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> service.createEvidenceFromKafka(
                record.topic(),
                record.key(),
                EvidenceType.KNOWLEDGE_ANSWER_FAILED,
                "knowledge-service",
                record.value(),
                KafkaMdcContext.tenantId(),
                KafkaMdcContext.correlationId()
        ));
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.payment-received}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentReceived(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            log.info(
                    "compliance_evidence_event_received type={} topic={} partition={} offset={} key={}",
                    EvidenceType.PAYMENT_RECEIVED,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );

            service.createEvidenceFromKafka(
                    record.topic(),
                    record.key(),
                    EvidenceType.PAYMENT_RECEIVED,
                    "payment-orchestration-service",
                    record.value(),
                    KafkaMdcContext.tenantId(),
                    KafkaMdcContext.correlationId()
            );
        });
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.payment-audit}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentAudit(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            log.info(
                    "compliance_evidence_event_received type={} topic={} partition={} offset={} key={}",
                    EvidenceType.PAYMENT_AUDIT,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );

            service.createEvidenceFromKafka(
                    record.topic(),
                    record.key(),
                    EvidenceType.PAYMENT_AUDIT,
                    "payment-orchestration-service",
                    record.value(),
                    KafkaMdcContext.tenantId(),
                    KafkaMdcContext.correlationId()
            );
        });
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.payment-status-changed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentStatusChanged(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            log.info(
                    "compliance_evidence_event_received type={} topic={} partition={} offset={} key={}",
                    EvidenceType.PAYMENT_STATUS_CHANGED,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );

            service.createEvidenceFromKafka(
                    record.topic(),
                    record.key(),
                    EvidenceType.PAYMENT_STATUS_CHANGED,
                    "payment-orchestration-service",
                    record.value(),
                    KafkaMdcContext.tenantId(),
                    KafkaMdcContext.correlationId()
            );
        });
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.payment-processed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentProcessed(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            String paymentId = extractPaymentId(record.value(), record.key());

            log.info(
                    "compliance_evidence_event_received type={} topic={} partition={} offset={} key={} paymentId={}",
                    EvidenceType.PAYMENT_COMPLETED,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    paymentId
            );

            service.createEvidenceFromKafka(
                    record.topic(),
                    record.key(),
                    EvidenceType.PAYMENT_COMPLETED,
                    "payment-orchestration-service",
                    record.value(),
                    KafkaMdcContext.tenantId(),
                    KafkaMdcContext.correlationId()
            );
        });
    }

    @KafkaListener(
            topics = "${naitfs.kafka.topics.payment-rejected}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentRejected(ConsumerRecord<String, String> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            String paymentId = extractPaymentId(record.value(), record.key());

            log.info(
                    "compliance_evidence_event_received type={} topic={} partition={} offset={} key={} paymentId={}",
                    EvidenceType.PAYMENT_DECISION_EVIDENCE,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    paymentId
            );

            service.createPaymentDecisionEvidenceFromKafka(
                    record.topic(),
                    record.key(),
                    record.value(),
                    KafkaMdcContext.tenantId(),
                    KafkaMdcContext.correlationId()
            );
        });
    }

    private String extractPaymentId(String payload, String fallback) {
        if (payload == null || payload.isBlank()) {
            return fallback;
        }

        try {
            String paymentId = objectMapper.readTree(payload).path("paymentId").asText(null);
            return paymentId == null || paymentId.isBlank() ? fallback : paymentId;
        } catch (Exception ex) {
            return fallback;
        }
    }
}
