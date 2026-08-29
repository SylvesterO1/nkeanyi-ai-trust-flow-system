package com.nkeanyi.compliance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.compliance.dto.CreateEvidenceRequest;
import com.nkeanyi.compliance.dto.DemoDashboardResponse;
import com.nkeanyi.compliance.dto.EvidenceCertificateResponse;
import com.nkeanyi.compliance.dto.EvidenceResponse;
import com.nkeanyi.compliance.dto.EvidenceSummaryResponse;
import com.nkeanyi.compliance.dto.TenantEvidenceSummaryResponse;
import com.nkeanyi.compliance.entity.ComplianceEvidence;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.repository.ComplianceEvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ComplianceEvidenceService {

    private final ComplianceEvidenceRepository repository;
    private final ObjectMapper objectMapper;

    public ComplianceEvidenceService(
            ComplianceEvidenceRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EvidenceResponse createEvidence(CreateEvidenceRequest request) {
        ComplianceEvidence evidence = new ComplianceEvidence();

        evidence.setTenantId(request.tenantId());
        evidence.setCorrelationId(request.correlationId());
        evidence.setDocumentId(request.documentId());
        evidence.setPaymentId(request.paymentId());
        evidence.setEvidenceType(request.evidenceType());
        evidence.setSourceService(request.sourceService());
        evidence.setSourceTopic(request.sourceTopic());
        evidence.setEventKey(request.eventKey());
        evidence.setSummary(request.summary());
        evidence.setRiskLevel(request.riskLevel());
        evidence.setDecision(request.decision());
        evidence.setActor(request.actor());
        evidence.setEvidencePayload(request.evidencePayload());
        evidence.setPayloadHash(sha256(request.evidencePayload()));
        evidence.setCreatedAt(Instant.now());
        evidence.setReceivedAt(Instant.now());
        evidence.setImmutableRecord(true);

        return toResponse(repository.save(evidence));
    }

    @Transactional
    public EvidenceResponse createEvidenceFromKafka(
            String topic,
            String eventKey,
            EvidenceType evidenceType,
            String sourceService,
            String rawPayload
    ) {
        return createEvidenceFromKafka(topic, eventKey, evidenceType, sourceService, rawPayload, null, null);
    }

    @Transactional
    public EvidenceResponse createEvidenceFromKafka(
            String topic,
            String eventKey,
            EvidenceType evidenceType,
            String sourceService,
            String rawPayload,
            String tenantIdFromHeaders,
            String correlationIdFromHeaders
    ) {
        JsonNode json = parseJson(rawPayload);

        String tenantId = firstPresent(tenantIdFromHeaders, readText(json, "tenantId", null), "default-tenant");
        String correlationId = firstPresent(correlationIdFromHeaders, readText(json, "correlationId", null), UUID.randomUUID().toString());
        String documentId = readText(json, "documentId", null);
        String paymentId = readText(json, "paymentId", null);
        String decision = resolveDecision(json, evidenceType);
        String riskLevel = resolveRiskLevel(json);
        String actor = readText(json, "actor", sourceService);

        String summary = buildSummary(evidenceType, documentId, paymentId, decision);

        CreateEvidenceRequest request = new CreateEvidenceRequest(
                tenantId,
                correlationId,
                documentId,
                paymentId,
                evidenceType,
                sourceService,
                topic,
                eventKey,
                summary,
                riskLevel,
                decision,
                actor,
                rawPayload
        );

        return createEvidence(request);
    }

    @Transactional
    public EvidenceResponse createPaymentDecisionEvidenceFromKafka(
            String topic,
            String eventKey,
            String rawPayload,
            String tenantIdFromHeaders,
            String correlationIdFromHeaders
    ) {
        JsonNode json = parseJson(rawPayload);

        String tenantId = firstPresent(tenantIdFromHeaders, readText(json, "tenantId", null), "default-tenant");
        String correlationId = firstPresent(correlationIdFromHeaders, readText(json, "correlationId", null), UUID.randomUUID().toString());
        String paymentId = readText(json, "paymentId", null);
        String customerId = readText(json, "customerId", null);
        String decision = resolveDecision(json, EvidenceType.PAYMENT_DECISION_EVIDENCE);
        String riskLevel = resolveRiskLevel(json);
        String amount = readText(json, "amount", null);
        String currency = readText(json, "currency", null);

        CreateEvidenceRequest request = new CreateEvidenceRequest(
                tenantId,
                correlationId,
                null,
                paymentId,
                EvidenceType.PAYMENT_DECISION_EVIDENCE,
                "payment-orchestration-service",
                topic,
                eventKey,
                buildPaymentDecisionSummary(paymentId, customerId, decision, riskLevel, amount, currency),
                riskLevel,
                decision,
                customerId,
                rawPayload
        );

        return createEvidence(request);
    }

    @Transactional(readOnly = true)
    public EvidenceResponse getById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Evidence record not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getByTenantId(String tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getByCorrelationId(String tenantId, String correlationId) {
        return repository.findByTenantIdAndCorrelationIdOrderByCreatedAtDesc(tenantId, correlationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvidenceSummaryResponse getCorrelationSummary(String tenantId, String correlationId) {
        List<EvidenceResponse> records = getByCorrelationId(tenantId, correlationId);
        int failedControls = countFailedControls(records);
        int successfulControls = records.size() - failedControls;
        Set<EvidenceType> evidenceTypes = records.stream()
                .map(EvidenceResponse::evidenceType)
                .collect(Collectors.toSet());
        List<EvidenceType> sortedEvidenceTypes = evidenceTypes.stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        List<String> sourceServices = sourceServices(records);
        String status = resolveTrustFlowStatus(records, evidenceTypes);

        return new EvidenceSummaryResponse(
                tenantId,
                correlationId,
                records.size(),
                sortedEvidenceTypes,
                sourceServices,
                firstEventTime(records),
                lastEventTime(records),
                status,
                records.size(),
                successfulControls,
                failedControls,
                sourceServices,
                "COMPLETE".equals(status) || "REVIEW_REQUIRED".equals(status) ? "COMPLETE" : "INCOMPLETE",
                failedControls > 0 ? "REVIEW_REQUIRED" : "LOW"
        );
    }

    @Transactional(readOnly = true)
    public TenantEvidenceSummaryResponse getTenantSummary(String tenantId) {
        List<EvidenceResponse> records = getByTenantId(tenantId);

        return new TenantEvidenceSummaryResponse(
                tenantId,
                records.size(),
                countCorrelations(records),
                evidenceCountByType(records),
                latestCorrelationIds(records),
                lastEventTime(records)
        );
    }

    @Transactional(readOnly = true)
    public DemoDashboardResponse getDemoDashboard(String tenantId) {
        List<EvidenceResponse> records = getByTenantId(tenantId);
        TenantEvidenceSummaryResponse summary = getTenantSummary(tenantId);
        List<DemoDashboardResponse.RecentTrustFlow> recentTrustFlows = records.stream()
                .filter(record -> record.correlationId() != null && !record.correlationId().isBlank())
                .collect(Collectors.groupingBy(
                        EvidenceResponse::correlationId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> recentTrustFlow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(
                        DemoDashboardResponse.RecentTrustFlow::lastEventTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(5)
                .toList();

        return new DemoDashboardResponse(
                tenantId,
                "compliance-evidence-service",
                Instant.now(),
                new DemoDashboardResponse.DashboardTotals(
                        summary.totalEvidence(),
                        summary.totalCorrelations(),
                        summary.latestEventTime()
                ),
                summary.evidenceCountByType(),
                recentTrustFlows,
                resolveReadinessStatus(summary.totalEvidence(), summary.totalCorrelations())
        );
    }

    @Transactional(readOnly = true)
    public EvidenceCertificateResponse getCorrelationCertificate(String tenantId, String correlationId) {
        List<EvidenceResponse> records = getByCorrelationId(tenantId, correlationId);
        Set<EvidenceType> evidenceTypes = records.stream()
                .map(EvidenceResponse::evidenceType)
                .collect(Collectors.toSet());
        boolean verified = isCompleteSuccessfulWorkflow(evidenceTypes);
        Instant completedAt = records.stream()
                .map(EvidenceResponse::createdAt)
                .filter(createdAt -> createdAt != null)
                .max(Instant::compareTo)
                .orElse(null);

        return new EvidenceCertificateResponse(
                "NAITFS_REGULATED_TRUST_FLOW",
                tenantId,
                correlationId,
                verified ? "VERIFIED" : "UNVERIFIED",
                completedAt,
                evidenceTypes.stream()
                        .map(Enum::name)
                        .filter(type -> !type.endsWith("_FAILED"))
                        .sorted()
                        .toList(),
                verified
                        ? "All required regulated trust-flow controls were verified."
                        : "Required regulated trust-flow controls are incomplete."
        );
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getByDocumentId(String documentId) {
        return repository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getByPaymentId(String tenantId, String paymentId) {
        return repository.findByTenantIdAndPaymentIdOrderByCreatedAtDesc(tenantId, paymentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getByEvidenceType(EvidenceType evidenceType) {
        return repository.findByEvidenceTypeOrderByCreatedAtDesc(evidenceType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private EvidenceResponse toResponse(ComplianceEvidence evidence) {
        return new EvidenceResponse(
                evidence.getId(),
                evidence.getTenantId(),
                evidence.getCorrelationId(),
                evidence.getDocumentId(),
                evidence.getPaymentId(),
                evidence.getEvidenceType(),
                evidence.getSourceService(),
                evidence.getSourceTopic(),
                evidence.getEventKey(),
                evidence.getSummary(),
                evidence.getRiskLevel(),
                evidence.getDecision(),
                evidence.getActor(),
                evidence.getPayloadHash(),
                evidence.getEvidencePayload(),
                evidence.getCreatedAt(),
                evidence.getReceivedAt(),
                evidence.isImmutableRecord()
        );
    }

    private JsonNode parseJson(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Kafka JSON payload", ex);
        }
    }

    private String readText(JsonNode json, String field, String defaultValue) {
        if (json == null || !json.hasNonNull(field)) {
            return defaultValue;
        }

        return json.get(field).asText();
    }

    private String resolveDecision(JsonNode json, EvidenceType evidenceType) {
        String explicitDecision = readText(json, "decision", null);
        if (explicitDecision != null && !explicitDecision.isBlank()) {
            return explicitDecision;
        }

        String status = readText(json, "status", null);
        if (status != null && !status.isBlank()) {
            return status;
        }

        if (evidenceType == EvidenceType.PAYMENT_STATUS_CHANGED) {
            return "STATUS_CHANGED";
        }

        return null;
    }

    private String resolveRiskLevel(JsonNode json) {
        String explicitRiskLevel = readText(json, "riskLevel", null);
        if (explicitRiskLevel != null && !explicitRiskLevel.isBlank()) {
            return explicitRiskLevel.toUpperCase();
        }

        Double riskScore = readDouble(json, "riskScore", null);
        if (riskScore == null) {
            return "UNKNOWN";
        }

        if (riskScore >= 80.0) {
            return "HIGH";
        }

        if (riskScore >= 50.0) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private Double readDouble(JsonNode json, String field, Double defaultValue) {
        if (json == null || !json.hasNonNull(field)) {
            return defaultValue;
        }

        JsonNode value = json.get(field);

        if (value.isNumber()) {
            return value.asDouble();
        }

        if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText());
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    private String buildSummary(
            EvidenceType evidenceType,
            String documentId,
            String paymentId,
            String decision
    ) {
        return switch (evidenceType) {
            case DOCUMENT_EXTRACTED -> "Document extraction evidence recorded for documentId=" + safe(documentId);
            case DOCUMENT_EXTRACTION_FAILED -> "Document extraction failure evidence recorded for documentId=" + safe(documentId);
            case KNOWLEDGE_ANSWER_GENERATED -> "Knowledge answer evidence recorded for documentId=" + safe(documentId);
            case KNOWLEDGE_ANSWER_FAILED -> "Knowledge answer failure evidence recorded for documentId=" + safe(documentId);
            case PAYMENT_RECEIVED -> "Payment received evidence recorded for paymentId=" + safe(paymentId);
            case PAYMENT_COMPLETED -> "Payment completed evidence recorded for paymentId=" + safe(paymentId);
            case PAYMENT_AUDIT -> "Payment audit evidence recorded for paymentId=" + safe(paymentId);
            case PAYMENT_STATUS_CHANGED -> "Payment status change evidence recorded for paymentId=" + safe(paymentId);
            case PAYMENT_DECISION_EVIDENCE -> "Payment decision evidence recorded for paymentId=" + safe(paymentId);
            case MANUAL_EVIDENCE -> "Manual compliance evidence recorded with decision=" + safe(decision);
        };
    }

    private String buildPaymentDecisionSummary(
            String paymentId,
            String customerId,
            String decision,
            String riskLevel,
            String amount,
            String currency
    ) {
        return "Payment decision evidence recorded for paymentId=" + safe(paymentId)
                + ", customerId=" + safe(customerId)
                + ", decision=" + safe(decision)
                + ", riskLevel=" + safe(riskLevel)
                + ", amount=" + safe(amount)
                + ", currency=" + safe(currency);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String firstPresent(String preferred, String fallback, String defaultValue) {
        if (preferred != null && !preferred.isBlank() && !preferred.startsWith("unknown-")) {
            return preferred;
        }

        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }

        return defaultValue;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash evidence payload", ex);
        }
    }

    private boolean isCompleteSuccessfulWorkflow(Set<EvidenceType> evidenceTypes) {
        return evidenceTypes.contains(EvidenceType.DOCUMENT_EXTRACTED)
                && evidenceTypes.contains(EvidenceType.KNOWLEDGE_ANSWER_GENERATED)
                && evidenceTypes.contains(EvidenceType.PAYMENT_RECEIVED)
                && evidenceTypes.contains(EvidenceType.PAYMENT_COMPLETED);
    }

    private int countFailedControls(List<EvidenceResponse> records) {
        return (int) records.stream()
                .filter(record -> record.evidenceType().name().endsWith("_FAILED"))
                .count();
    }

    private Map<String, Long> evidenceCountByType(List<EvidenceResponse> records) {
        return records.stream()
                .collect(Collectors.groupingBy(
                        record -> record.evidenceType().name(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private List<String> latestCorrelationIds(List<EvidenceResponse> records) {
        return records.stream()
                .filter(record -> record.correlationId() != null && !record.correlationId().isBlank())
                .sorted(Comparator.comparing(EvidenceResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(EvidenceResponse::correlationId)
                .distinct()
                .limit(5)
                .toList();
    }

    private int countCorrelations(List<EvidenceResponse> records) {
        return (int) records.stream()
                .map(EvidenceResponse::correlationId)
                .filter(correlationId -> correlationId != null && !correlationId.isBlank())
                .distinct()
                .count();
    }

    private Instant firstEventTime(List<EvidenceResponse> records) {
        return records.stream()
                .map(EvidenceResponse::createdAt)
                .filter(createdAt -> createdAt != null)
                .min(Instant::compareTo)
                .orElse(null);
    }

    private Instant lastEventTime(List<EvidenceResponse> records) {
        return records.stream()
                .map(EvidenceResponse::createdAt)
                .filter(createdAt -> createdAt != null)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private List<String> sourceServices(List<EvidenceResponse> records) {
        return records.stream()
                .map(EvidenceResponse::sourceService)
                .filter(sourceService -> sourceService != null && !sourceService.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private String resolveTrustFlowStatus(List<EvidenceResponse> records, Set<EvidenceType> evidenceTypes) {
        if (records.isEmpty()) {
            return "NO_EVIDENCE";
        }

        if (countFailedControls(records) > 0) {
            return "REVIEW_REQUIRED";
        }

        if (isCompleteSuccessfulWorkflow(evidenceTypes)) {
            return "COMPLETE";
        }

        return "INCOMPLETE";
    }

    private DemoDashboardResponse.RecentTrustFlow recentTrustFlow(String correlationId, List<EvidenceResponse> records) {
        Set<EvidenceType> evidenceTypes = records.stream()
                .map(EvidenceResponse::evidenceType)
                .collect(Collectors.toSet());

        return new DemoDashboardResponse.RecentTrustFlow(
                correlationId,
                records.size(),
                evidenceTypes.stream()
                        .map(Enum::name)
                        .sorted()
                        .toList(),
                sourceServices(records),
                lastEventTime(records),
                resolveTrustFlowStatus(records, evidenceTypes)
        );
    }

    private String resolveReadinessStatus(int totalEvidence, int totalCorrelations) {
        if (totalEvidence == 0 || totalCorrelations == 0) {
            return "NO_DEMO_DATA";
        }

        return "READY";
    }
}
