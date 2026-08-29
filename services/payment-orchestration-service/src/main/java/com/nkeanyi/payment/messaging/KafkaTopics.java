package com.nkeanyi.payment.messaging;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String PAYMENT_RECEIVED = "payment.received";
    public static final String FRAUD_ASSESSED = "fraud.assessed";
    public static final String COMPLIANCE_ASSESSED = "compliance.assessed";
    public static final String PAYMENT_PROCESSED = "payment.processed";
    public static final String PAYMENT_REJECTED = "payment.rejected";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_RECEIVED_DLT = "payment.received.dlt";
}
