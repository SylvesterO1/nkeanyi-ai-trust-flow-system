package com.nkeanyi.documentintelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_audit_events")
public class DocumentAuditEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "result", nullable = false)
    private String result;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected DocumentAuditEvent() {
    }

    public DocumentAuditEvent(String tenantId, String documentId, String actorId, String action, String result, String reason, String correlationId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.actorId = actorId;
        this.action = action;
        this.result = result;
        this.reason = reason;
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String documentId() {
        return documentId;
    }

    public String actorId() {
        return actorId;
    }

    public String action() {
        return action;
    }

    public String result() {
        return result;
    }

    public String reason() {
        return reason;
    }

    public String correlationId() {
        return correlationId;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
