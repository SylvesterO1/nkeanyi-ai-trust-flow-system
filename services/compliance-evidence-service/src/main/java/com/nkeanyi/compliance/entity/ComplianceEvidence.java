package com.nkeanyi.compliance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "compliance_evidence",
        indexes = {
                @Index(name = "idx_evidence_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_evidence_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_evidence_document_id", columnList = "document_id"),
                @Index(name = "idx_evidence_payment_id", columnList = "payment_id"),
                @Index(name = "idx_evidence_type", columnList = "evidence_type"),
                @Index(name = "idx_evidence_created_at", columnList = "created_at")
        }
)
public class ComplianceEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "correlation_id", nullable = false, length = 150)
    private String correlationId;

    @Column(name = "document_id", length = 150)
    private String documentId;

    @Column(name = "payment_id", length = 150)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 80)
    private EvidenceType evidenceType;

    @Column(name = "source_service", nullable = false, length = 120)
    private String sourceService;

    @Column(name = "source_topic", length = 180)
    private String sourceTopic;

    @Column(name = "event_key", length = 180)
    private String eventKey;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Column(name = "decision", length = 100)
    private String decision;

    @Column(name = "actor", length = 150)
    private String actor;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Column(name = "evidence_payload", nullable = false, columnDefinition = "TEXT")
    private String evidencePayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "immutable_record", nullable = false)
    private boolean immutableRecord = true;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (receivedAt == null) {
            receivedAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public EvidenceType getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(EvidenceType evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getEvidencePayload() {
        return evidencePayload;
    }

    public void setEvidencePayload(String evidencePayload) {
        this.evidencePayload = evidencePayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public boolean isImmutableRecord() {
        return immutableRecord;
    }

    public void setImmutableRecord(boolean immutableRecord) {
        this.immutableRecord = immutableRecord;
    }
}
