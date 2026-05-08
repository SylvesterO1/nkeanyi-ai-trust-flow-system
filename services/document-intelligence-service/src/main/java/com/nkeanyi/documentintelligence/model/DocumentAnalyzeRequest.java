package com.nkeanyi.documentintelligence.model;

import jakarta.validation.constraints.NotBlank;

public record DocumentAnalyzeRequest(
        @NotBlank String documentName,
        @NotBlank String documentType
) {}
