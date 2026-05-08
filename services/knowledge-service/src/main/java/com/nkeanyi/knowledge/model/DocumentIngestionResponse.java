package com.nkeanyi.knowledge.model;

public record DocumentIngestionResponse(
        String objectName,
        String bucket,
        int chunksIndexed,
        String status
) {}
