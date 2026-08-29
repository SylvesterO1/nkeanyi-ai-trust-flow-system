package com.nkeanyi.knowledge.kafka;

import com.nkeanyi.knowledge.citation.CitationPackage;

import java.time.Instant;

public record KnowledgeAnswerGeneratedEvent(
        String tenantId,
        String correlationId,
        String idempotencyKey,
        String documentId,
        String query,
        String answer,
        CitationPackage citationPackage,
        Instant generatedAt
) {}
