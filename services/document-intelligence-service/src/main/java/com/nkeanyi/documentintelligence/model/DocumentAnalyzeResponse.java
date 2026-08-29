package com.nkeanyi.documentintelligence.model;

import java.util.Map;

public record DocumentAnalyzeResponse(
        String documentName,
        String documentType,
        String status,
        Map<String, Object> extractedFields
) {}
