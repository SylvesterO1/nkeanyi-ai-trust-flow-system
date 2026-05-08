package com.nkeanyi.compliance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.compliance.dto.CreateEvidenceRequest;
import com.nkeanyi.compliance.dto.EvidenceResponse;
import com.nkeanyi.compliance.entity.ComplianceEvidence;
import com.nkeanyi.compliance.entity.EvidenceType;
import com.nkeanyi.compliance.repository.ComplianceEvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

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
        JsonNode json = parseJson(rawPayload);

        String tenantId = readText(json, "tenantId", "default-tenant");
        String correlationId = readText(json, "correlationId", UUID.randomUUID().toString());
        String documentId = readText(json, "documentId", null);
        String paymentId = readText(json, "paymentId", null);
        String decision = readText(json, "decision", null);
        String riskLevel = readText(json, "riskLevel", null);
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
    public List<EvidenceResponse> getByCorrelationId(String correlationId) {
        return repository.findByCorrelationIdOrderByCreatedAtDesc(correlationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getByDocumentId(String documentId) {
        return repository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getByPaymentId(String paymentId) {
        return repository.findByPaymentIdOrderByCreatedAtDesc(paymentId)
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

    private String buildSummary(
            EvidenceType evidenceType,
            String documentId,
            String paymentId,
            String decision
    ) {
        return switch (evidenceType) {
            case DOCUMENT_EXTRACTED -> "Document extraction evidence recorded for documentId=" + safe(documentId);
            case KNOWLEDGE_ANSWER_GENERATED -> "Knowledge answer evidence recorded for documentId=" + safe(documentId);
            case PAYMENT_AUDIT -> "Payment audit evidence recorded for paymentId=" + safe(paymentId);
            case PAYMENT_STATUS_CHANGED -> "Payment status change evidence recorded for paymentId=" + safe(paymentId);
            case MANUAL_EVIDENCE -> "Manual compliance evidence recorded with decision=" + safe(decision);
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
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
}
