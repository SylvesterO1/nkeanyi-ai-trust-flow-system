package com.nkeanyi.compliance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.compliance.dto.EvidenceCertificateResponse;
import com.nkeanyi.compliance.dto.EvidenceResponse;
import com.nkeanyi.compliance.dto.EvidenceSummaryResponse;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.repository.ComplianceEvidenceRepository;
import com.nkeanyi.compliance.service.ComplianceEvidenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComplianceEvidenceControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ComplianceEvidenceController controller = new ComplianceEvidenceController(new TenantScopedEvidenceService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getByPaymentIdReturnsBadRequestWhenTenantHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/payment/payment-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Id header is required"));
    }

    @Test
    void getByCorrelationIdReturnsBadRequestWhenTenantHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/correlation/corr-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Id header is required"));
    }

    @Test
    void getByPaymentIdReturnsEvidenceForCorrectTenant() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/payment/payment-123")
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.evidence[0].tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.evidence[0].paymentId").value("payment-123"));
    }

    @Test
    void getByCorrelationIdReturnsEvidenceForCorrectTenant() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/correlation/corr-123")
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.evidence[0].tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.evidence[0].correlationId").value("corr-123"));
    }

    @Test
    void getByPaymentIdReturnsZeroRecordsForWrongTenant() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/payment/payment-123")
                        .header("X-Tenant-Id", "tenant-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.evidence").isEmpty());
    }

    @Test
    void getByCorrelationIdReturnsZeroRecordsForWrongTenant() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/correlation/corr-123")
                        .header("X-Tenant-Id", "tenant-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.evidence").isArray())
                .andExpect(jsonPath("$.evidence").isEmpty());
    }

    @Test
    void getCorrelationSummaryReturnsCompleteForSuccessfulEvidencePackage() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/correlation/corr-success/summary")
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.correlationId").value("corr-success"))
                .andExpect(jsonPath("$.totalEvidence").value(4))
                .andExpect(jsonPath("$.evidenceTypes").isArray())
                .andExpect(jsonPath("$.sourceServices").isArray())
                .andExpect(jsonPath("$.firstEventTime").exists())
                .andExpect(jsonPath("$.lastEventTime").exists())
                .andExpect(jsonPath("$.status").value("COMPLETE"))
                .andExpect(jsonPath("$.totalEvidenceCount").value(4))
                .andExpect(jsonPath("$.successfulControls").value(4))
                .andExpect(jsonPath("$.failedControls").value(0))
                .andExpect(jsonPath("$.auditStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.riskStatus").value("LOW"))
                .andExpect(jsonPath("$.servicesInvolved").isArray());
    }

    @Test
    void getCorrelationCertificateReturnsVerifiedForSuccessfulEvidencePackage() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/correlation/corr-success/certificate")
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateType").value("NAITFS_REGULATED_TRUST_FLOW"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.correlationId").value("corr-success"))
                .andExpect(jsonPath("$.workflowStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.controlsVerified").isArray());
    }

    @Test
    void getCorrelationSummaryReturnsIncompleteForEmptyCorrelationResult() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/correlation/corr-empty/summary")
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvidenceCount").value(0))
                .andExpect(jsonPath("$.successfulControls").value(0))
                .andExpect(jsonPath("$.failedControls").value(0))
                .andExpect(jsonPath("$.auditStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.riskStatus").value("LOW"));
    }

    @Test
    void getCorrelationSummaryFlagsMixedSuccessAndFailureEvidence() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/correlation/corr-mixed/summary")
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvidenceCount").value(5))
                .andExpect(jsonPath("$.successfulControls").value(4))
                .andExpect(jsonPath("$.failedControls").value(1))
                .andExpect(jsonPath("$.auditStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.riskStatus").value("REVIEW_REQUIRED"));
    }

    @Test
    void getTenantSummaryReturnsTenantMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/tenant/tenant-a/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.totalEvidence").value(4))
                .andExpect(jsonPath("$.totalCorrelations").value(1))
                .andExpect(jsonPath("$.evidenceCountByType.DOCUMENT_EXTRACTED").value(1))
                .andExpect(jsonPath("$.latestCorrelationIds[0]").value("corr-success"))
                .andExpect(jsonPath("$.latestEventTime").exists());
    }

    @Test
    void getDemoDashboardReturnsDashboardResponseShape() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/demo-dashboard")
                        .param("tenantId", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.serviceName").value("compliance-evidence-service"))
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.totals.totalEvidence").value(4))
                .andExpect(jsonPath("$.evidenceByType.PAYMENT_COMPLETED").value(1))
                .andExpect(jsonPath("$.recentTrustFlows").isArray())
                .andExpect(jsonPath("$.recentTrustFlows[0].correlationId").value("corr-success"))
                .andExpect(jsonPath("$.readinessStatus").value("READY"));
    }

    private static class TenantScopedEvidenceService extends ComplianceEvidenceService {

        TenantScopedEvidenceService() {
            super(repositoryProxy(), new ObjectMapper());
        }

        @Override
        public List<EvidenceResponse> getByPaymentId(String tenantId, String paymentId) {
            if ("tenant-a".equals(tenantId) && "payment-123".equals(paymentId)) {
                return List.of(evidence(tenantId, "corr-123", paymentId));
            }

            return List.of();
        }

        @Override
        public List<EvidenceResponse> getByCorrelationId(String tenantId, String correlationId) {
            if ("tenant-a".equals(tenantId) && "corr-success".equals(correlationId)) {
                return successfulPackage(tenantId, correlationId);
            }

            if ("tenant-a".equals(tenantId) && "corr-mixed".equals(correlationId)) {
                return mixedPackage(tenantId, correlationId);
            }

            if ("tenant-a".equals(tenantId) && "corr-123".equals(correlationId)) {
                return List.of(evidence(tenantId, correlationId, "payment-123"));
            }

            return List.of();
        }

        @Override
        public List<EvidenceResponse> getByTenantId(String tenantId) {
            if ("tenant-a".equals(tenantId)) {
                return successfulPackage(tenantId, "corr-success");
            }

            return List.of();
        }

        @Override
        public EvidenceSummaryResponse getCorrelationSummary(String tenantId, String correlationId) {
            return super.getCorrelationSummary(tenantId, correlationId);
        }

        @Override
        public EvidenceCertificateResponse getCorrelationCertificate(String tenantId, String correlationId) {
            return super.getCorrelationCertificate(tenantId, correlationId);
        }

        private List<EvidenceResponse> successfulPackage(String tenantId, String correlationId) {
            return List.of(
                    evidence(tenantId, correlationId, null, EvidenceType.DOCUMENT_EXTRACTED, "document-intelligence-service"),
                    evidence(tenantId, correlationId, null, EvidenceType.KNOWLEDGE_ANSWER_GENERATED, "knowledge-service"),
                    evidence(tenantId, correlationId, "payment-123", EvidenceType.PAYMENT_RECEIVED, "payment-orchestration-service"),
                    evidence(tenantId, correlationId, "payment-123", EvidenceType.PAYMENT_COMPLETED, "payment-orchestration-service")
            );
        }

        private List<EvidenceResponse> mixedPackage(String tenantId, String correlationId) {
            var records = new java.util.ArrayList<>(successfulPackage(tenantId, correlationId));
            records.add(evidence(tenantId, correlationId, null, EvidenceType.KNOWLEDGE_ANSWER_FAILED, "knowledge-service"));
            return records;
        }

        private EvidenceResponse evidence(String tenantId, String correlationId, String paymentId) {
            return evidence(tenantId, correlationId, paymentId, EvidenceType.PAYMENT_DECISION_EVIDENCE, "payment-orchestration-service");
        }

        private EvidenceResponse evidence(
                String tenantId,
                String correlationId,
                String paymentId,
                EvidenceType evidenceType,
                String sourceService
        ) {
            return new EvidenceResponse(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    tenantId,
                    correlationId,
                    null,
                    paymentId,
                    evidenceType,
                    sourceService,
                    "payment.processed",
                    paymentId,
                    "test evidence",
                    "LOW",
                    "APPROVED",
                    "cust-123",
                    "hash",
                    "{}",
                    Instant.parse("2026-05-12T00:00:00Z"),
                    Instant.parse("2026-05-12T00:00:01Z"),
                    true
            );
        }

        private static ComplianceEvidenceRepository repositoryProxy() {
            return (ComplianceEvidenceRepository) Proxy.newProxyInstance(
                    ComplianceEvidenceRepository.class.getClassLoader(),
                    new Class<?>[] {ComplianceEvidenceRepository.class},
                    (proxy, method, args) -> {
                        if ("toString".equals(method.getName())) {
                            return "ComplianceEvidenceRepository unused controller test proxy";
                        }

                        throw new UnsupportedOperationException(method.getName() + " is not used in this test");
                    }
            );
        }
    }
}
