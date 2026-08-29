package com.nkeanyi.compliance.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TenantEvidenceSummaryResponse(
        String tenantId,
        int totalEvidence,
        int totalCorrelations,
        Map<String, Long> evidenceCountByType,
        List<String> latestCorrelationIds,
        Instant latestEventTime
) {
}
