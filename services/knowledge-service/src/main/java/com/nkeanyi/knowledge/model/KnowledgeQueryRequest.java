package com.nkeanyi.knowledge.model;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeQueryRequest(
        @NotBlank String query
) {}
