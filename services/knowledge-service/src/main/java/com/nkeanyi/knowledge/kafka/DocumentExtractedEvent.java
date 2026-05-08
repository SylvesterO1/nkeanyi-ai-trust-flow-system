package com.nkeanyi.knowledge.kafka;

import java.util.List;

public record DocumentExtractedEvent(
        String objectName,
        String bucket,
        String contentType,
        String extractedText,
        String documentType,
        String extractorUsed,
        String extractionMode,
        boolean ocrUsed,
        double confidence,
        List<PagePayload> pages
) {
    public record PagePayload(
            int pageNumber,
            String sectionTitle,
            String text,
            int charCount
    ) {}
}
