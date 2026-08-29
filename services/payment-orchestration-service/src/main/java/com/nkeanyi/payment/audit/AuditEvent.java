package com.nkeanyi.payment.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(
        String eventType,
        String actor,
        String resourceType,
        String resourceId,
        String outcome,
        Instant timestamp,
        Map<String, Object> metadata
) {
}
