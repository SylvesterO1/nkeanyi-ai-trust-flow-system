package com.nkeanyi.knowledge.model;

public record KnowledgeEvaluationResponse(
        int totalEvents,
        int groundedAnswers,
        int blockedAnswers,
        int noEvidenceAnswers,
        double groundingRate,
        double blockRate,
        double averageConfidence
) {}
