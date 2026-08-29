package com.nkeanyi.documentintelligence.model;

public enum DocumentLifecycleState {
    RECEIVED,
    CLASSIFIED,
    PARSED,
    ENRICHED,
    IN_REVIEW,
    APPROVED,
    PUBLISHED,
    DEPRECATED,
    QUARANTINED,
    FAILED
}
