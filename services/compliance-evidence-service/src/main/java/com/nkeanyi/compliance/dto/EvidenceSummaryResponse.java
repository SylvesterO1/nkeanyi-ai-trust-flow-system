package com.nkeanyi.compliance.dto;

import com.nkeanyi.compliance.entity.EvidenceType;

import java.time.Instant;
import java.util.List;

public record EvidenceSummaryResponse(
        String tenantId,
        String correlationId,
        int totalEvidence,
        List<EvidenceType> evidenceTypes,
        List<String> sourceServices,
        Instant firstEventTime,
        Instant lastEventTime,
        String status,
        int totalEvidenceCount,
        int successfulControls,
        int failedControls,
        List<String> servicesInvolved,
        String auditStatus,
        String riskStatus
) {
}
