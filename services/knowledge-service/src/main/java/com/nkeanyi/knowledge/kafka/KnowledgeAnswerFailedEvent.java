package com.nkeanyi.knowledge.kafka;

import java.time.Instant;

public record KnowledgeAnswerFailedEvent(
        String tenantId,
        String correlationId,
        String idempotencyKey,
        String documentId,
        String query,
        String errorType,
        String errorCode,
        String errorMessage,
        boolean recoverable,
        String sourceService,
        Instant timestamp
) {}
