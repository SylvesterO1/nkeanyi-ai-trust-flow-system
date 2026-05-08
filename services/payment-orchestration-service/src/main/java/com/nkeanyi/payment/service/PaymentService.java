package com.nkeanyi.payment.service;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.PaymentCreateResult;
import com.nkeanyi.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentCreateResult createPayment(String idempotencyKey, CreatePaymentRequest request);
    PaymentResponse getPaymentById(Long id);
    List<PaymentResponse> getAllPayments();
}
