package com.nkeanyi.payment.service;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.OrchestrationDecision;
import org.springframework.stereotype.Service;

@Service
public class ComplianceScreeningService {

    public OrchestrationDecision screen(CreatePaymentRequest request) {
        if (request.getNarration() != null) {
            String narration = request.getNarration().toLowerCase();

            if (narration.contains("sanction") || narration.contains("blocked entity")) {
                return new OrchestrationDecision(false, "COMPLIANCE_REVIEW",
                        "Payment flagged by compliance screening", 90.0);
            }
        }

        return new OrchestrationDecision(true, "COMPLIANCE_REVIEW",
                "Compliance screening passed", 5.0);
    }
}
