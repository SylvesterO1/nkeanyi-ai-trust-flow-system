package com.nkeanyi.documentintelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_feedback_events")
public class DocumentFeedbackEvent {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "severity")
    private String severity;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentFeedbackEvent() {
    }

    public DocumentFeedbackEvent(String documentId, String tenantId, String userId, String category, String severity, String message) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.category = category;
        this.severity = severity;
        this.message = message;
        this.status = "OPEN";
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

    public String userId() {
        return userId;
    }

    public String category() {
        return category;
    }

    public String severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public String status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
