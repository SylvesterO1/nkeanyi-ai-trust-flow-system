package com.nkeanyi.payment.service;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import org.springframework.stereotype.Service;

@Service
public class PaymentValidationService {

    public void validate(CreatePaymentRequest request) {
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (request.getSourceAccount() == null || request.getSourceAccount().isBlank()) {
            throw new IllegalArgumentException("sourceAccount is required");
        }
        if (request.getDestinationAccount() == null || request.getDestinationAccount().isBlank()) {
            throw new IllegalArgumentException("destinationAccount is required");
        }
        if (request.getSourceAccount().equals(request.getDestinationAccount())) {
            throw new IllegalArgumentException("source and destination accounts cannot be the same");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        if (!request.getCurrency().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("currency must be a valid 3-letter code");
        }
    }
}
