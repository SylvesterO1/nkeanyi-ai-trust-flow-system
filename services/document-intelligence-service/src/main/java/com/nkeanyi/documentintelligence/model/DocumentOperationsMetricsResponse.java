package com.nkeanyi.documentintelligence.model;

import java.util.Map;

public record DocumentOperationsMetricsResponse(
        long totalDocuments,
        long openReviewTasks,
        long quarantinedDocuments,
        long feedbackEvents,
        Map<String, Long> documentsByLifecycleState,
        Map<String, Long> reviewTasksByStatus
) {}
