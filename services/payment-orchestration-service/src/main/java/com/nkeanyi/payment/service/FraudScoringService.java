package com.nkeanyi.payment.service;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.OrchestrationDecision;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FraudScoringService {

    public OrchestrationDecision evaluate(CreatePaymentRequest request) {
        double score = 0.0;

        if (request.getAmount() != null && request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            score += 55.0;
        }

        if (request.getPaymentMethod() != null && request.getPaymentMethod().equalsIgnoreCase("CRYPTO")) {
            score += 30.0;
        }

        if (request.getNarration() != null &&
                request.getNarration().toLowerCase().contains("urgent")) {
            score += 10.0;
        }

        if (score >= 70.0) {
            return new OrchestrationDecision(false, "FRAUD_REVIEW", "High fraud risk detected", score);
        }

        return new OrchestrationDecision(true, "FRAUD_REVIEW", "Fraud screening passed", score);
    }
}
