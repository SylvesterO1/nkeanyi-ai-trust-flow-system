package com.nkeanyi.knowledge.model;

import java.util.List;

public record KnowledgeQueryResponse(
        String query,
        List<ChunkResult> chunks
) {
    public record ChunkResult(
            String id,
            String source,
            String content,
            int pageNumber,
            String documentType,
            String sectionTitle,
            String extractorUsed,
            String extractionMode,
            boolean ocrUsed,
            double score,
            double confidence
    ) {}
}
