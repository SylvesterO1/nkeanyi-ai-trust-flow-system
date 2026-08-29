package com.nkeanyi.documentintelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_workflow_states")
public class DocumentWorkflowState {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "workspace_id")
    private String workspaceId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "source_uri")
    private String sourceUri;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "sensitivity_label")
    private String sensitivityLabel;

    @Column(name = "access_policy")
    private String accessPolicy;

    @Column(name = "lifecycle_state", nullable = false)
    private String lifecycleState;

    @Column(name = "review_status")
    private String reviewStatus;

    @Column(name = "reviewer_id")
    private String reviewerId;

    @Column(name = "review_decision")
    private String reviewDecision;

    @Column(name = "decision_rationale", columnDefinition = "TEXT")
    private String decisionRationale;

    @Column(name = "quarantine_reason", columnDefinition = "TEXT")
    private String quarantineReason;

    @Column(name = "replacement_document_id")
    private String replacementDocumentId;

    @Column(name = "retention_policy")
    private String retentionPolicy;

    @Column(name = "quality_gate_status")
    private String qualityGateStatus;

    @Column(name = "quality_gate_findings", columnDefinition = "TEXT")
    private String qualityGateFindings;

    @Column(name = "extraction_confidence")
    private Double extractionConfidence;

    @Column(name = "ocr_used")
    private Boolean ocrUsed;

    @Column(name = "extractor_used")
    private String extractorUsed;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentWorkflowState() {
    }

    public DocumentWorkflowState(
            String documentId,
            String tenantId,
            String workspaceId,
            String correlationId,
            String sourceUri,
            String checksum,
            String ownerId,
            String originalFilename,
            String contentType,
            String documentType,
            String sensitivityLabel,
            String accessPolicy,
            String lifecycleState,
            String reviewStatus,
            String quarantineReason,
            String retentionPolicy,
            String qualityGateStatus,
            String qualityGateFindings,
            Double extractionConfidence,
            Boolean ocrUsed,
            String extractorUsed,
            Instant publishedAt,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.tenantId = tenantId;
        this.workspaceId = workspaceId;
        this.correlationId = correlationId;
        this.sourceUri = sourceUri;
        this.checksum = checksum;
        this.ownerId = ownerId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.documentType = documentType;
        this.sensitivityLabel = sensitivityLabel;
        this.accessPolicy = accessPolicy;
        this.lifecycleState = lifecycleState;
        this.reviewStatus = reviewStatus;
        this.quarantineReason = quarantineReason;
        this.retentionPolicy = retentionPolicy;
        this.qualityGateStatus = qualityGateStatus;
        this.qualityGateFindings = qualityGateFindings;
        this.extractionConfidence = extractionConfidence;
        this.ocrUsed = ocrUsed;
        this.extractorUsed = extractorUsed;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String documentId() {
        return documentId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String workspaceId() {
        return workspaceId;
    }

    public String correlationId() {
        return correlationId;
    }

    public String sourceUri() {
        return sourceUri;
    }

    public String checksum() {
        return checksum;
    }

    public String ownerId() {
        return ownerId;
    }

    public String originalFilename() {
        return originalFilename;
    }

    public String contentType() {
        return contentType;
    }

    public String documentType() {
        return documentType;
    }

    public String sensitivityLabel() {
        return sensitivityLabel;
    }

    public String accessPolicy() {
        return accessPolicy;
    }

    public String lifecycleState() {
        return lifecycleState;
    }

    public String reviewStatus() {
        return reviewStatus;
    }

    public String reviewerId() {
        return reviewerId;
    }

    public String reviewDecision() {
        return reviewDecision;
    }

    public String decisionRationale() {
        return decisionRationale;
    }

    public String quarantineReason() {
        return quarantineReason;
    }

    public String replacementDocumentId() {
        return replacementDocumentId;
    }

    public String retentionPolicy() {
        return retentionPolicy;
    }

    public String qualityGateStatus() {
        return qualityGateStatus;
    }

    public String qualityGateFindings() {
        return qualityGateFindings;
    }

    public Double extractionConfidence() {
        return extractionConfidence;
    }

    public Boolean ocrUsed() {
        return ocrUsed;
    }

    public String extractorUsed() {
        return extractorUsed;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    public Instant deprecatedAt() {
        return deprecatedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void applyReview(String reviewerId, String decision, String rationale, String ownerId, String retentionPolicy) {
        this.reviewerId = reviewerId;
        this.reviewDecision = decision;
        this.decisionRationale = rationale;
        if (ownerId != null && !ownerId.isBlank()) {
            this.ownerId = ownerId;
        }
        if (retentionPolicy != null && !retentionPolicy.isBlank()) {
            this.retentionPolicy = retentionPolicy;
        }
        this.updatedAt = Instant.now();
    }

    public void transition(String lifecycleState, String reviewStatus, String reason) {
        this.lifecycleState = lifecycleState;
        this.reviewStatus = reviewStatus;
        if (reason != null && !reason.isBlank()) {
            this.quarantineReason = reason;
        }
        this.updatedAt = Instant.now();
    }

    public void publish(String actorId) {
        this.lifecycleState = "PUBLISHED";
        this.reviewStatus = "RESOLVED";
        this.reviewerId = actorId;
        this.publishedAt = Instant.now();
        this.updatedAt = this.publishedAt;
    }

    public void deprecate(String actorId, String replacementDocumentId, String rationale) {
        this.lifecycleState = "DEPRECATED";
        this.reviewStatus = "RESOLVED";
        this.reviewerId = actorId;
        this.replacementDocumentId = replacementDocumentId;
        this.decisionRationale = rationale;
        this.deprecatedAt = Instant.now();
        this.updatedAt = this.deprecatedAt;
    }
}
