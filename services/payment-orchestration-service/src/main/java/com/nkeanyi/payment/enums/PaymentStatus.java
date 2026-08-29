package com.nkeanyi.payment.enums;

public enum PaymentStatus {
    RECEIVED,
    VALIDATING,
    COMPLIANCE_REVIEW,
    ANOMALY_REVIEW,
    FRAUD_REVIEW,
    APPROVED,
    PROCESSING,
    COMPLETED,
    REJECTED,
    FAILED
}
