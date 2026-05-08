package com.nkeanyi.documentintelligence.model;

public record DocumentProcessResponse(
        String objectName,
        String bucket,
        String extractedText,
        int extractedTextLength,
        String extractorUsed,
        boolean ocrUsed,
        String extractionMode,
        double confidence,
        String eventStatus,
        String status
) {}