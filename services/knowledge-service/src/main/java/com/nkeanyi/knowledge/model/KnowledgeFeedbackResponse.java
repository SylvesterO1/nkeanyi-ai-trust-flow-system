package com.nkeanyi.knowledge.model;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeFeedbackResponse(
        UUID id,
        String retrievalEventId,
        String documentId,
        String tenantId,
        String userId,
        String category,
        String severity,
        String message,
        String status,
        Instant createdAt
) {}
