package com.nkeanyi.compliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.compliance.entity.ComplianceEvidence;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.repository.ComplianceEvidenceRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceEvidenceServiceTest {

    @Test
    void getCorrelationSummaryAggregatesMultipleEvidenceRecords() {
        String correlationId = "corr-mvp13";
        List<ComplianceEvidence> records = List.of(
                evidence("tenant-a", correlationId, EvidenceType.PAYMENT_COMPLETED, "payment-orchestration-service", "2026-05-12T00:03:00Z"),
                evidence("tenant-a", correlationId, EvidenceType.DOCUMENT_EXTRACTED, "document-intelligence-service", "2026-05-12T00:01:00Z"),
                evidence("tenant-a", correlationId, EvidenceType.KNOWLEDGE_ANSWER_GENERATED, "knowledge-service", "2026-05-12T00:02:00Z"),
                evidence("tenant-a", correlationId, EvidenceType.PAYMENT_RECEIVED, "payment-orchestration-service", "2026-05-12T00:02:30Z")
        );
        ComplianceEvidenceService service = new ComplianceEvidenceService(repositoryProxy(records, List.of()), new ObjectMapper());

        var summary = service.getCorrelationSummary("tenant-a", correlationId);

        assertThat(summary.tenantId()).isEqualTo("tenant-a");
        assertThat(summary.correlationId()).isEqualTo(correlationId);
        assertThat(summary.totalEvidence()).isEqualTo(4);
        assertThat(summary.evidenceTypes()).containsExactly(
                EvidenceType.DOCUMENT_EXTRACTED,
                EvidenceType.KNOWLEDGE_ANSWER_GENERATED,
                EvidenceType.PAYMENT_COMPLETED,
                EvidenceType.PAYMENT_RECEIVED
        );
        assertThat(summary.sourceServices()).containsExactly(
                "document-intelligence-service",
                "knowledge-service",
                "payment-orchestration-service"
        );
        assertThat(summary.firstEventTime()).isEqualTo(Instant.parse("2026-05-12T00:01:00Z"));
        assertThat(summary.lastEventTime()).isEqualTo(Instant.parse("2026-05-12T00:03:00Z"));
        assertThat(summary.status()).isEqualTo("COMPLETE");
        assertThat(summary.auditStatus()).isEqualTo("COMPLETE");
    }

    @Test
    void getCorrelationSummaryReturnsNoEvidenceForEmptyCorrelation() {
        ComplianceEvidenceService service = new ComplianceEvidenceService(repositoryProxy(List.of(), List.of()), new ObjectMapper());

        var summary = service.getCorrelationSummary("tenant-a", "corr-empty");

        assertThat(summary.totalEvidence()).isZero();
        assertThat(summary.evidenceTypes()).isEmpty();
        assertThat(summary.sourceServices()).isEmpty();
        assertThat(summary.firstEventTime()).isNull();
        assertThat(summary.lastEventTime()).isNull();
        assertThat(summary.status()).isEqualTo("NO_EVIDENCE");
        assertThat(summary.auditStatus()).isEqualTo("INCOMPLETE");
    }

    @Test
    void getTenantSummaryAggregatesTenantEvidence() {
        List<ComplianceEvidence> tenantRecords = List.of(
                evidence("tenant-a", "corr-new", EvidenceType.PAYMENT_COMPLETED, "payment-orchestration-service", "2026-05-12T00:04:00Z"),
                evidence("tenant-a", "corr-old", EvidenceType.DOCUMENT_EXTRACTED, "document-intelligence-service", "2026-05-12T00:01:00Z"),
                evidence("tenant-a", "corr-new", EvidenceType.PAYMENT_RECEIVED, "payment-orchestration-service", "2026-05-12T00:03:00Z")
        );
        ComplianceEvidenceService service = new ComplianceEvidenceService(repositoryProxy(List.of(), tenantRecords), new ObjectMapper());

        var summary = service.getTenantSummary("tenant-a");

        assertThat(summary.tenantId()).isEqualTo("tenant-a");
        assertThat(summary.totalEvidence()).isEqualTo(3);
        assertThat(summary.totalCorrelations()).isEqualTo(2);
        assertThat(summary.evidenceCountByType()).containsAllEntriesOf(Map.of(
                "PAYMENT_COMPLETED", 1L,
                "DOCUMENT_EXTRACTED", 1L,
                "PAYMENT_RECEIVED", 1L
        ));
        assertThat(summary.latestCorrelationIds()).containsExactly("corr-new", "corr-old");
        assertThat(summary.latestEventTime()).isEqualTo(Instant.parse("2026-05-12T00:04:00Z"));
    }

    @Test
    void summaryQueriesRemainTenantIsolated() {
        String correlationId = "corr-shared";
        List<ComplianceEvidence> records = List.of(
                evidence("tenant-a", correlationId, EvidenceType.PAYMENT_COMPLETED, "payment-orchestration-service", "2026-05-12T00:01:00Z"),
                evidence("tenant-b", correlationId, EvidenceType.DOCUMENT_EXTRACTION_FAILED, "document-intelligence-service", "2026-05-12T00:02:00Z")
        );
        ComplianceEvidenceService service = new ComplianceEvidenceService(repositoryProxy(records, records), new ObjectMapper());

        var tenantASummary = service.getCorrelationSummary("tenant-a", correlationId);
        var tenantBSummary = service.getTenantSummary("tenant-b");

        assertThat(tenantASummary.totalEvidence()).isEqualTo(1);
        assertThat(tenantASummary.evidenceTypes()).containsExactly(EvidenceType.PAYMENT_COMPLETED);
        assertThat(tenantBSummary.totalEvidence()).isEqualTo(1);
        assertThat(tenantBSummary.evidenceCountByType()).containsEntry("DOCUMENT_EXTRACTION_FAILED", 1L);
    }

    @Test
    void getDemoDashboardReturnsDashboardReadyShape() {
        List<ComplianceEvidence> tenantRecords = List.of(
                evidence("tenant-a", "corr-dashboard", EvidenceType.DOCUMENT_EXTRACTED, "document-intelligence-service", "2026-05-12T00:01:00Z"),
                evidence("tenant-a", "corr-dashboard", EvidenceType.PAYMENT_RECEIVED, "payment-orchestration-service", "2026-05-12T00:02:00Z")
        );
        ComplianceEvidenceService service = new ComplianceEvidenceService(repositoryProxy(List.of(), tenantRecords), new ObjectMapper());

        var dashboard = service.getDemoDashboard("tenant-a");

        assertThat(dashboard.tenantId()).isEqualTo("tenant-a");
        assertThat(dashboard.serviceName()).isEqualTo("compliance-evidence-service");
        assertThat(dashboard.generatedAt()).isNotNull();
        assertThat(dashboard.totals().totalEvidence()).isEqualTo(2);
        assertThat(dashboard.evidenceByType()).containsEntry("DOCUMENT_EXTRACTED", 1L);
        assertThat(dashboard.recentTrustFlows()).singleElement()
                .satisfies(flow -> {
                    assertThat(flow.correlationId()).isEqualTo("corr-dashboard");
                    assertThat(flow.totalEvidence()).isEqualTo(2);
                    assertThat(flow.status()).isEqualTo("INCOMPLETE");
                });
        assertThat(dashboard.readinessStatus()).isEqualTo("READY");
    }

    @Test
    void getByCorrelationIdDoesNotReturnEvidenceForAnotherTenant() {
        String correlationId = "corr-123";
        ComplianceEvidence tenantEvidence = evidence("tenant-a", correlationId);
        List<String> repositoryCalls = new ArrayList<>();
        ComplianceEvidenceRepository repository = repositoryProxy(repositoryCalls, tenantEvidence);
        ComplianceEvidenceService service = new ComplianceEvidenceService(repository, new ObjectMapper());

        var wrongTenantRecords = service.getByCorrelationId("tenant-b", correlationId);
        var correctTenantRecords = service.getByCorrelationId("tenant-a", correlationId);

        assertThat(wrongTenantRecords).isEmpty();
        assertThat(correctTenantRecords)
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.tenantId()).isEqualTo("tenant-a");
                    assertThat(record.correlationId()).isEqualTo(correlationId);
                });

        assertThat(repositoryCalls).containsExactly(
                "tenant-b:" + correlationId,
                "tenant-a:" + correlationId
        );
    }

    @Test
    void createPaymentDecisionEvidenceFromKafkaSavesPaymentDecisionEvidence() {
        AtomicReference<ComplianceEvidence> savedEvidence = new AtomicReference<>();
        ComplianceEvidenceRepository repository = repositoryProxyForSave(savedEvidence);
        ComplianceEvidenceService service = new ComplianceEvidenceService(repository, new ObjectMapper());
        String payload = """
                {
                  "paymentId": "pay-123",
                  "customerId": "cust-123",
                  "decision": "APPROVED",
                  "riskLevel": "LOW",
                  "amount": 125.50,
                  "currency": "USD"
                }
                """;

        var response = service.createPaymentDecisionEvidenceFromKafka(
                "payment.processed",
                "pay-123",
                payload,
                "tenant-a",
                "corr-123"
        );

        ComplianceEvidence evidence = savedEvidence.get();
        assertThat(evidence).isNotNull();
        assertThat(evidence.getTenantId()).isEqualTo("tenant-a");
        assertThat(evidence.getCorrelationId()).isEqualTo("corr-123");
        assertThat(evidence.getPaymentId()).isEqualTo("pay-123");
        assertThat(evidence.getEvidenceType()).isEqualTo(EvidenceType.PAYMENT_DECISION_EVIDENCE);
        assertThat(evidence.getSourceService()).isEqualTo("payment-orchestration-service");
        assertThat(evidence.getSourceTopic()).isEqualTo("payment.processed");
        assertThat(evidence.getEventKey()).isEqualTo("pay-123");
        assertThat(evidence.getDecision()).isEqualTo("APPROVED");
        assertThat(evidence.getRiskLevel()).isEqualTo("LOW");
        assertThat(evidence.getActor()).isEqualTo("cust-123");
        assertThat(evidence.getPayloadHash()).isNotBlank();
        assertThat(evidence.getSummary())
                .contains("paymentId=pay-123")
                .contains("customerId=cust-123")
                .contains("decision=APPROVED")
                .contains("riskLevel=LOW")
                .contains("amount=125.5")
                .contains("currency=USD");
        assertThat(response.evidenceType()).isEqualTo(EvidenceType.PAYMENT_DECISION_EVIDENCE);
    }

    private ComplianceEvidenceRepository repositoryProxy(
            List<String> repositoryCalls,
            ComplianceEvidence tenantEvidence
    ) {
        return (ComplianceEvidenceRepository) Proxy.newProxyInstance(
                ComplianceEvidenceRepository.class.getClassLoader(),
                new Class<?>[] {ComplianceEvidenceRepository.class},
                (proxy, method, args) -> {
                    if ("findByTenantIdAndCorrelationIdOrderByCreatedAtDesc".equals(method.getName())) {
                        String tenantId = (String) args[0];
                        String correlationId = (String) args[1];
                        repositoryCalls.add(tenantId + ":" + correlationId);

                        if ("tenant-a".equals(tenantId)) {
                            return List.of(tenantEvidence);
                        }

                        return List.of();
                    }

                    if ("toString".equals(method.getName())) {
                        return "ComplianceEvidenceRepository test proxy";
                    }

                    throw new UnsupportedOperationException(method.getName() + " is not used in this test");
                }
        );
    }

    private ComplianceEvidenceRepository repositoryProxy(
            List<ComplianceEvidence> correlationRecords,
            List<ComplianceEvidence> tenantRecords
    ) {
        return (ComplianceEvidenceRepository) Proxy.newProxyInstance(
                ComplianceEvidenceRepository.class.getClassLoader(),
                new Class<?>[] {ComplianceEvidenceRepository.class},
                (proxy, method, args) -> {
                    if ("findByTenantIdAndCorrelationIdOrderByCreatedAtDesc".equals(method.getName())) {
                        String tenantId = (String) args[0];
                        String correlationId = (String) args[1];

                        return correlationRecords.stream()
                                .filter(record -> tenantId.equals(record.getTenantId()))
                                .filter(record -> correlationId.equals(record.getCorrelationId()))
                                .toList();
                    }

                    if ("findByTenantIdOrderByCreatedAtDesc".equals(method.getName())) {
                        String tenantId = (String) args[0];

                        return tenantRecords.stream()
                                .filter(record -> tenantId.equals(record.getTenantId()))
                                .toList();
                    }

                    if ("toString".equals(method.getName())) {
                        return "ComplianceEvidenceRepository summary test proxy";
                    }

                    throw new UnsupportedOperationException(method.getName() + " is not used in this test");
                }
        );
    }

    private ComplianceEvidenceRepository repositoryProxyForSave(AtomicReference<ComplianceEvidence> savedEvidence) {
        return (ComplianceEvidenceRepository) Proxy.newProxyInstance(
                ComplianceEvidenceRepository.class.getClassLoader(),
                new Class<?>[] {ComplianceEvidenceRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        ComplianceEvidence evidence = (ComplianceEvidence) args[0];
                        evidence.prePersist();
                        savedEvidence.set(evidence);
                        return evidence;
                    }

                    if ("toString".equals(method.getName())) {
                        return "ComplianceEvidenceRepository save test proxy";
                    }

                    throw new UnsupportedOperationException(method.getName() + " is not used in this test");
                }
        );
    }

    private ComplianceEvidence evidence(String tenantId, String correlationId) {
        return evidence(tenantId, correlationId, EvidenceType.PAYMENT_AUDIT, "test-service", "2026-05-12T00:00:00Z");
    }

    private ComplianceEvidence evidence(
            String tenantId,
            String correlationId,
            EvidenceType evidenceType,
            String sourceService,
            String createdAt
    ) {
        ComplianceEvidence evidence = new ComplianceEvidence();
        evidence.setTenantId(tenantId);
        evidence.setCorrelationId(correlationId);
        evidence.setPaymentId("payment-123");
        evidence.setEvidenceType(evidenceType);
        evidence.setSourceService(sourceService);
        evidence.setSummary("test evidence");
        evidence.setPayloadHash("hash");
        evidence.setEvidencePayload("{}");
        evidence.setCreatedAt(Instant.parse(createdAt));
        evidence.setReceivedAt(Instant.parse("2026-05-12T00:00:01Z"));
        evidence.setImmutableRecord(true);
        return evidence;
    }
}
