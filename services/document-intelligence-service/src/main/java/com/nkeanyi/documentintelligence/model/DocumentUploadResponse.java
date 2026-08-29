package com.nkeanyi.documentintelligence.model;

public record DocumentUploadResponse(
        String objectName,
        String bucket,
        String status
) {}
