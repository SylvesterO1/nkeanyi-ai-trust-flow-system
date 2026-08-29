package com.nkeanyi.documentintelligence.model;

import java.time.Instant;
import java.util.UUID;

public record DocumentReviewTaskResponse(
        UUID id,
        String documentId,
        String tenantId,
        String status,
        String reason,
        String riskLevel,
        String assignedRole,
        String reviewerId,
        String decision,
        String rationale,
        Instant createdAt,
        Instant resolvedAt
) {}
