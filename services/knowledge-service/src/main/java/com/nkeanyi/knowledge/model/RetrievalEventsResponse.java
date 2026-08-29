package com.nkeanyi.knowledge.model;

import java.util.List;

public record RetrievalEventsResponse(
        List<RetrievalEventResponse> events,
        int count
) {}
