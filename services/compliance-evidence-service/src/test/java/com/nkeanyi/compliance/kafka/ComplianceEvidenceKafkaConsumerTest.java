package com.nkeanyi.compliance.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.compliance.dto.EvidenceResponse;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.repository.ComplianceEvidenceRepository;
import com.nkeanyi.compliance.service.ComplianceEvidenceService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceEvidenceKafkaConsumerTest {

    @Test
    void consumePaymentProcessedCreatesPaymentCompletedEvidence() {
        CapturingComplianceEvidenceService service = new CapturingComplianceEvidenceService();
        ComplianceEvidenceKafkaConsumer consumer = new ComplianceEvidenceKafkaConsumer(service, new ObjectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment.processed",
                0,
                42L,
                "pay-123",
                "{\"paymentId\":\"pay-123\",\"customerId\":\"cust-123\",\"decision\":\"APPROVED\",\"riskLevel\":\"LOW\",\"amount\":125.50,\"currency\":\"USD\"}"
        );
        record.headers().add("X-Tenant-Id", "tenant-a".getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Correlation-Id", "corr-123".getBytes(StandardCharsets.UTF_8));

        consumer.consumePaymentProcessed(record);

        assertThat(service.topic).isEqualTo("payment.processed");
        assertThat(service.eventKey).isEqualTo("pay-123");
        assertThat(service.evidenceType).isEqualTo(EvidenceType.PAYMENT_COMPLETED);
        assertThat(service.sourceService).isEqualTo("payment-orchestration-service");
        assertThat(service.rawPayload).contains("\"customerId\":\"cust-123\"");
        assertThat(service.tenantId).isEqualTo("tenant-a");
        assertThat(service.correlationId).isEqualTo("corr-123");
    }

    @Test
    void consumePaymentReceivedCreatesPaymentReceivedEvidence() {
        CapturingComplianceEvidenceService service = new CapturingComplianceEvidenceService();
        ComplianceEvidenceKafkaConsumer consumer = new ComplianceEvidenceKafkaConsumer(service, new ObjectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment.received",
                0,
                44L,
                "pay-123",
                "{\"paymentId\":\"pay-123\",\"customerId\":\"cust-123\",\"idempotencyKey\":\"idem-123\"}"
        );
        record.headers().add("X-Tenant-Id", "tenant-a".getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Correlation-Id", "corr-789".getBytes(StandardCharsets.UTF_8));

        consumer.consumePaymentReceived(record);

        assertThat(service.topic).isEqualTo("payment.received");
        assertThat(service.eventKey).isEqualTo("pay-123");
        assertThat(service.evidenceType).isEqualTo(EvidenceType.PAYMENT_RECEIVED);
        assertThat(service.sourceService).isEqualTo("payment-orchestration-service");
        assertThat(service.rawPayload).contains("\"idempotencyKey\":\"idem-123\"");
        assertThat(service.tenantId).isEqualTo("tenant-a");
        assertThat(service.correlationId).isEqualTo("corr-789");
    }

    @Test
    void consumeKnowledgeAnswerGeneratedCreatesKnowledgeEvidence() {
        CapturingComplianceEvidenceService service = new CapturingComplianceEvidenceService();
        ComplianceEvidenceKafkaConsumer consumer = new ComplianceEvidenceKafkaConsumer(service, new ObjectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "knowledge.answer.generated.v1",
                0,
                43L,
                "doc-123",
                "{\"documentId\":\"doc-123\",\"query\":\"What is required?\",\"answer\":\"Sanctions screening [1]\"}"
        );
        record.headers().add("X-Tenant-Id", "tenant-a".getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Correlation-Id", "corr-456".getBytes(StandardCharsets.UTF_8));

        consumer.consumeKnowledgeAnswerGenerated(record);

        assertThat(service.topic).isEqualTo("knowledge.answer.generated.v1");
        assertThat(service.eventKey).isEqualTo("doc-123");
        assertThat(service.evidenceType).isEqualTo(EvidenceType.KNOWLEDGE_ANSWER_GENERATED);
        assertThat(service.sourceService).isEqualTo("knowledge-service");
        assertThat(service.rawPayload).contains("\"documentId\":\"doc-123\"");
        assertThat(service.tenantId).isEqualTo("tenant-a");
        assertThat(service.correlationId).isEqualTo("corr-456");
    }

    @Test
    void consumeDocumentExtractionFailedCreatesFailureEvidence() {
        CapturingComplianceEvidenceService service = new CapturingComplianceEvidenceService();
        ComplianceEvidenceKafkaConsumer consumer = new ComplianceEvidenceKafkaConsumer(service, new ObjectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "document.extraction.failed.v1",
                0,
                45L,
                "idem-doc",
                "{\"idempotencyKey\":\"idem-doc\",\"documentName\":\"bad.pdf\",\"errorType\":\"IllegalStateException\"}"
        );
        record.headers().add("X-Tenant-Id", "tenant-a".getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Correlation-Id", "corr-doc-fail".getBytes(StandardCharsets.UTF_8));

        consumer.consumeDocumentExtractionFailed(record);

        assertThat(service.evidenceType).isEqualTo(EvidenceType.DOCUMENT_EXTRACTION_FAILED);
        assertThat(service.sourceService).isEqualTo("document-intelligence-service");
        assertThat(service.rawPayload).contains("\"documentName\":\"bad.pdf\"");
    }

    @Test
    void consumeKnowledgeAnswerFailedCreatesFailureEvidence() {
        CapturingComplianceEvidenceService service = new CapturingComplianceEvidenceService();
        ComplianceEvidenceKafkaConsumer consumer = new ComplianceEvidenceKafkaConsumer(service, new ObjectMapper());
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "knowledge.answer.failed.v1",
                0,
                46L,
                "doc-123",
                "{\"documentId\":\"doc-123\",\"query\":\"Question?\",\"errorType\":\"NO_GROUNDED_CHUNKS\"}"
        );
        record.headers().add("X-Tenant-Id", "tenant-a".getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Correlation-Id", "corr-knowledge-fail".getBytes(StandardCharsets.UTF_8));

        consumer.consumeKnowledgeAnswerFailed(record);

        assertThat(service.evidenceType).isEqualTo(EvidenceType.KNOWLEDGE_ANSWER_FAILED);
        assertThat(service.sourceService).isEqualTo("knowledge-service");
        assertThat(service.rawPayload).contains("\"documentId\":\"doc-123\"");
    }

    private static class CapturingComplianceEvidenceService extends ComplianceEvidenceService {

        private String topic;
        private String eventKey;
        private EvidenceType evidenceType;
        private String sourceService;
        private String rawPayload;
        private String tenantId;
        private String correlationId;

        CapturingComplianceEvidenceService() {
            super(repositoryProxy(), new ObjectMapper());
        }

        @Override
        public EvidenceResponse createEvidenceFromKafka(
                String topic,
                String eventKey,
                EvidenceType evidenceType,
                String sourceService,
                String rawPayload,
                String tenantIdFromHeaders,
                String correlationIdFromHeaders
        ) {
            this.topic = topic;
            this.eventKey = eventKey;
            this.evidenceType = evidenceType;
            this.sourceService = sourceService;
            this.rawPayload = rawPayload;
            this.tenantId = tenantIdFromHeaders;
            this.correlationId = correlationIdFromHeaders;
            return null;
        }

        @Override
        public EvidenceResponse createPaymentDecisionEvidenceFromKafka(
                String topic,
                String eventKey,
                String rawPayload,
                String tenantIdFromHeaders,
                String correlationIdFromHeaders
        ) {
            this.topic = topic;
            this.eventKey = eventKey;
            this.rawPayload = rawPayload;
            this.tenantId = tenantIdFromHeaders;
            this.correlationId = correlationIdFromHeaders;
            return null;
        }

        private static ComplianceEvidenceRepository repositoryProxy() {
            return (ComplianceEvidenceRepository) Proxy.newProxyInstance(
                    ComplianceEvidenceRepository.class.getClassLoader(),
                    new Class<?>[] {ComplianceEvidenceRepository.class},
                    (proxy, method, args) -> {
                        if ("toString".equals(method.getName())) {
                            return "ComplianceEvidenceRepository unused test proxy";
                        }

                        throw new UnsupportedOperationException(method.getName() + " is not used in this test");
                    }
            );
        }
    }
}
