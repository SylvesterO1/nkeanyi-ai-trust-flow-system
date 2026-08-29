package com.nkeanyi.documentintelligence.model;

import java.time.Instant;
import java.util.UUID;

public record DocumentAuditEventResponse(
        UUID id,
        String tenantId,
        String documentId,
        String actorId,
        String action,
        String result,
        String reason,
        String correlationId,
        Instant timestamp
) {}
