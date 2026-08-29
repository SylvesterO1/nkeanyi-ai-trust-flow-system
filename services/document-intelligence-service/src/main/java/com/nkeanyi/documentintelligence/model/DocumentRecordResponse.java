package com.nkeanyi.documentintelligence.model;

import java.time.Instant;

public record DocumentRecordResponse(
        String documentId,
        String tenantId,
        String correlationId,
        String originalFilename,
        String contentType,
        String extractorUsed,
        Double confidence,
        String status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        String extractedText
) {
    public DocumentRecordResponse withoutExtractedText() {
        return new DocumentRecordResponse(
                documentId,
                tenantId,
                correlationId,
                originalFilename,
                contentType,
                extractorUsed,
                confidence,
                status,
                errorMessage,
                createdAt,
                updatedAt,
                null
        );
    }
}
