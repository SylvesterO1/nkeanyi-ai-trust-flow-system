package com.nkeanyi.knowledge.model;

public record DocumentUploadResponse(
        String objectName,
        String bucket,
        String status
) {}
