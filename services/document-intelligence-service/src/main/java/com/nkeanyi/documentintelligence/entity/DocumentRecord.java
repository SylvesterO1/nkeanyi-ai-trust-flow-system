package com.nkeanyi.documentintelligence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_records")
public class DocumentRecord {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "extractor_used")
    private String extractorUsed;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentRecord() {
    }

    public DocumentRecord(
            String documentId,
            String tenantId,
            String correlationId,
            String originalFilename,
            String contentType,
            String extractorUsed,
            Double confidence,
            String status,
            String extractedText,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.tenantId = tenantId;
        this.correlationId = correlationId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.extractorUsed = extractorUsed;
        this.confidence = confidence;
        this.status = status;
        this.extractedText = extractedText;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String documentId() {
        return documentId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String correlationId() {
        return correlationId;
    }

    public String originalFilename() {
        return originalFilename;
    }

    public String contentType() {
        return contentType;
    }

    public String extractorUsed() {
        return extractorUsed;
    }

    public Double confidence() {
        return confidence;
    }

    public String status() {
        return status;
    }

    public String extractedText() {
        return extractedText;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
