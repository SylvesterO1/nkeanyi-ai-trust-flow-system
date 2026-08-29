package com.nkeanyi.compliance.dto;

import java.time.Instant;
import java.util.List;

public record EvidenceCertificateResponse(
        String certificateType,
        String tenantId,
        String correlationId,
        String workflowStatus,
        Instant completedAt,
        List<String> controlsVerified,
        String message
) {
}
