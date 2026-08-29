package com.nkeanyi.documentintelligence.model;

public record DocumentProcessResponse(
        String objectName,
        String documentId,
        String bucket,
        String tenantId,
        String correlationId,
        String originalFilename,
        String contentType,
        String extractedText,
        int extractedTextLength,
        String extractorUsed,
        boolean ocrUsed,
        String extractionMode,
        double confidence,
        String eventStatus,
        String status
) {}
