package com.nkeanyi.payment.ai;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.time.Instant;
import java.util.Map;

public record AiDecisionRecord(
        String useCase,
        String modelName,
        String modelVersion,
        String inputReference,
        String outputReference,
        Double confidenceScore,
        boolean humanOverrideRequired,
        String finalDecision,
        Instant timestamp,
        Map<String, Object> metadata
) {

    public AiDecisionRecord {
        useCase = requireText(useCase, "useCase");
        modelName = requireText(modelName, "modelName");
        modelVersion = requireText(modelVersion, "modelVersion");
        inputReference = requireText(inputReference, "inputReference");
        outputReference = requireText(outputReference, "outputReference");
        finalDecision = requireText(finalDecision, "finalDecision");
        timestamp = Objects.requireNonNull(timestamp, "timestamp is required");

        if (confidenceScore != null && (confidenceScore < 0.0 || confidenceScore > 1.0)) {
            throw new IllegalArgumentException("confidenceScore must be between 0.0 and 1.0");
        }

        metadata = metadata == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(metadata));
    }

    public boolean hasConfidenceScore() {
        return confidenceScore != null;
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(finalDecision);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
