package com.nkeanyi.payment.service;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.entity.Payment;

public interface PaymentOrchestrationService {
    Payment orchestrate(Payment payment, CreatePaymentRequest request);
}
