package com.nkeanyi.documentintelligence.model;

import java.time.Instant;
import java.util.List;

public record DocumentWorkflowResponse(
        String documentId,
        String tenantId,
        String workspaceId,
        String correlationId,
        String sourceUri,
        String checksum,
        String ownerId,
        String originalFilename,
        String contentType,
        String documentType,
        String sensitivityLabel,
        String accessPolicy,
        String lifecycleState,
        String reviewStatus,
        String reviewerId,
        String reviewDecision,
        String decisionRationale,
        String quarantineReason,
        String replacementDocumentId,
        String retentionPolicy,
        String qualityGateStatus,
        List<String> qualityGateFindings,
        Double extractionConfidence,
        Boolean ocrUsed,
        String extractorUsed,
        Instant publishedAt,
        Instant deprecatedAt,
        Instant createdAt,
        Instant updatedAt
) {}
