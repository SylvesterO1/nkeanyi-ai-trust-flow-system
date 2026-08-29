package com.nkeanyi.compliance.dto;

import com.nkeanyi.compliance.entity.EvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEvidenceRequest(

        @NotBlank
        String tenantId,

        @NotBlank
        String correlationId,

        String documentId,

        String paymentId,

        @NotNull
        EvidenceType evidenceType,

        @NotBlank
        String sourceService,

        String sourceTopic,

        String eventKey,

        @NotBlank
        String summary,

        String riskLevel,

        String decision,

        String actor,

        @NotBlank
        String evidencePayload
) {
}
