package com.nkeanyi.documentintelligence.model;

import jakarta.validation.constraints.NotBlank;

public record DocumentReviewRequest(
        @NotBlank String reviewerId,
        @NotBlank String decision,
        String rationale,
        String ownerId,
        String retentionPolicy,
        String replacementDocumentId
) {}
