package com.nkeanyi.compliance.dto;

import com.nkeanyi.compliance.entity.EvidenceType;

import java.time.Instant;
import java.util.UUID;

public record EvidenceResponse(
        UUID id,
        String tenantId,
        String correlationId,
        String documentId,
        String paymentId,
        EvidenceType evidenceType,
        String sourceService,
        String sourceTopic,
        String eventKey,
        String summary,
        String riskLevel,
        String decision,
        String actor,
        String payloadHash,
        String evidencePayload,
        Instant createdAt,
        Instant receivedAt,
        boolean immutableRecord
) {
}
