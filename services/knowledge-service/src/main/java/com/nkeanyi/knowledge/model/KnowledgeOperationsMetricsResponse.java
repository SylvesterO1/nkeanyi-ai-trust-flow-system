package com.nkeanyi.knowledge.model;

import java.util.Map;

public record KnowledgeOperationsMetricsResponse(
        long retrievalEvents,
        long feedbackEvents,
        long blockedAnswers,
        long noEvidenceAnswers,
        Map<String, Long> eventsByStatus
) {}
