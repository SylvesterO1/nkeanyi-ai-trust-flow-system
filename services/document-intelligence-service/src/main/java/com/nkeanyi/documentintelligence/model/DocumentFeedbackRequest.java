package com.nkeanyi.documentintelligence.model;

import jakarta.validation.constraints.NotBlank;

public record DocumentFeedbackRequest(
        @NotBlank String userId,
        @NotBlank String category,
        String message,
        String severity
) {}
