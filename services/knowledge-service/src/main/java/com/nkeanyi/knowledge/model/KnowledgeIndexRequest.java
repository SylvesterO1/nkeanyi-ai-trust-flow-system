package com.nkeanyi.knowledge.model;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeIndexRequest(
        @NotBlank String source,
        @NotBlank String content
) {}
