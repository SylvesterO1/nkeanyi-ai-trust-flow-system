package com.nkeanyi.knowledge.model;

import java.time.Instant;
import java.util.UUID;

public record RetrievalEventResponse(
        UUID id,
        String tenantId,
        String workspaceId,
        String userId,
        String documentId,
        String query,
        String status,
        String blockedReason,
        int citationCount,
        Double confidence,
        long latencyMs,
        Instant createdAt
) {}
