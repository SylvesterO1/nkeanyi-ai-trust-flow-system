package com.nkeanyi.knowledge.citation;

public record Citation(
        int index,
        String chunkId,
        String source,
        int pageNumber,
        String documentType,
        String sectionTitle,
        String excerpt,
        double score
) {}
