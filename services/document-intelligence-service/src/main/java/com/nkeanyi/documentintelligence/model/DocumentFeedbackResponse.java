package com.nkeanyi.documentintelligence.model;

import java.time.Instant;
import java.util.UUID;

public record DocumentFeedbackResponse(
        UUID id,
        String documentId,
        String tenantId,
        String userId,
        String category,
        String severity,
        String message,
        String status,
        Instant createdAt
) {}
