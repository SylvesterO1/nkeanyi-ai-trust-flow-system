package com.nkeanyi.documentintelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_review_tasks")
public class DocumentReviewTask {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "assigned_role")
    private String assignedRole;

    @Column(name = "reviewer_id")
    private String reviewerId;

    @Column(name = "decision")
    private String decision;

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected DocumentReviewTask() {
    }

    public DocumentReviewTask(String documentId, String tenantId, String reason, String riskLevel, String assignedRole) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.tenantId = tenantId;
        this.status = "OPEN";
        this.reason = reason;
        this.riskLevel = riskLevel;
        this.assignedRole = assignedRole;
        this.createdAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String documentId() {
        return documentId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public String riskLevel() {
        return riskLevel;
    }

    public String assignedRole() {
        return assignedRole;
    }

    public String reviewerId() {
        return reviewerId;
    }

    public String decision() {
        return decision;
    }

    public String rationale() {
        return rationale;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public void resolve(String reviewerId, String decision, String rationale) {
        this.status = "RESOLVED";
        this.reviewerId = reviewerId;
        this.decision = decision;
        this.rationale = rationale;
        this.resolvedAt = Instant.now();
    }
}
