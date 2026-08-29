package com.nkeanyi.knowledge.model;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeFeedbackRequest(
        String retrievalEventId,
        String documentId,
        @NotBlank String userId,
        @NotBlank String category,
        String message,
        String severity
) {}
