package com.nkeanyi.payment.dto;

public class PaymentCreateResult {

    private final PaymentResponse payment;
    private final boolean newlyCreated;

    public PaymentCreateResult(PaymentResponse payment, boolean newlyCreated) {
        this.payment = payment;
        this.newlyCreated = newlyCreated;
    }

    public PaymentResponse getPayment() {
        return payment;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }
}
