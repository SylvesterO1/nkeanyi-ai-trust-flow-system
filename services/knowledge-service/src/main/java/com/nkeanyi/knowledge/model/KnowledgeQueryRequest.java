package com.nkeanyi.knowledge.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record KnowledgeQueryRequest(
        @NotBlank String query,
        String documentId,
        String tenantId,
        String workspaceId,
        String userId,
        List<String> roles,
        List<String> authorizedDocumentIds,
        String sensitivityClearance
) {}
