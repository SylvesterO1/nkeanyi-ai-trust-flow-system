package com.nkeanyi.knowledge.model;

import com.nkeanyi.knowledge.citation.CitationPackage;

public record KnowledgeAnswerResponse(
        String query,
        String answer,
        CitationPackage citationPackage,
        String status,
        String blockedReason,
        Double confidence,
        String retrievalEventId
) {
    public KnowledgeAnswerResponse(String query, String answer, CitationPackage citationPackage) {
        this(query, answer, citationPackage, "ANSWERED", null, null, null);
    }
}
