package com.nkeanyi.documentintelligence.kafka;

import java.time.Instant;

public record DocumentExtractionFailedEvent(
        String tenantId,
        String correlationId,
        String idempotencyKey,
        String documentName,
        String errorType,
        String errorCode,
        String errorMessage,
        boolean recoverable,
        String sourceService,
        Instant timestamp
) {}
