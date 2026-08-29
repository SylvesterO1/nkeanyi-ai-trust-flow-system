package com.nkeanyi.compliance.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DemoDashboardResponse(
        String tenantId,
        String serviceName,
        Instant generatedAt,
        DashboardTotals totals,
        Map<String, Long> evidenceByType,
        List<RecentTrustFlow> recentTrustFlows,
        String readinessStatus
) {
    public record DashboardTotals(
            int totalEvidence,
            int totalCorrelations,
            Instant latestEventTime
    ) {
    }

    public record RecentTrustFlow(
            String correlationId,
            int totalEvidence,
            List<String> evidenceTypes,
            List<String> sourceServices,
            Instant lastEventTime,
            String status
    ) {
    }
}
