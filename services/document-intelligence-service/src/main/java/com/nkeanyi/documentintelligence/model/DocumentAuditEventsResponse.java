package com.nkeanyi.documentintelligence.model;

import java.util.List;

public record DocumentAuditEventsResponse(
        List<DocumentAuditEventResponse> events,
        int count
) {}
