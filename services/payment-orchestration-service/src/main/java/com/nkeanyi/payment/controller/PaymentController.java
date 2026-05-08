package com.nkeanyi.payment.controller;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.PaymentCreateResult;
import com.nkeanyi.payment.dto.PaymentResponse;
import com.nkeanyi.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_payments.write')")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreatePaymentRequest request) {

        PaymentCreateResult result = paymentService.createPayment(idempotencyKey, request);

        HttpStatus status = result.isNewlyCreated()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(result.getPayment());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_payments.read')")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_payments.read')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
