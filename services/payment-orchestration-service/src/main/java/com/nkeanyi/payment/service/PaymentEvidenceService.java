package com.nkeanyi.payment.service;

import com.nkeanyi.payment.entity.LedgerEntry;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.entity.PaymentRiskDecision;
import com.nkeanyi.payment.entity.PaymentStatusEvent;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.repository.LedgerEntryRepository;
import com.nkeanyi.payment.repository.PaymentRiskDecisionRepository;
import com.nkeanyi.payment.repository.PaymentStatusEventRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentEvidenceService {

    private final PaymentStatusEventRepository statusEventRepository;
    private final PaymentRiskDecisionRepository riskDecisionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public PaymentEvidenceService(PaymentStatusEventRepository statusEventRepository,
                                  PaymentRiskDecisionRepository riskDecisionRepository,
                                  LedgerEntryRepository ledgerEntryRepository) {
        this.statusEventRepository = statusEventRepository;
        this.riskDecisionRepository = riskDecisionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public void recordInitialPaymentEvidence(Payment payment) {
        statusEventRepository.save(new PaymentStatusEvent(
                payment.getId(),
                payment.getPaymentId(),
                null,
                PaymentStatus.RECEIVED,
                "Payment request received",
                "payment-orchestration-service"
        ));

        riskDecisionRepository.save(new PaymentRiskDecision(
                payment.getId(),
                payment.getPaymentId(),
                payment.getRiskScore() == null ? 0.0 : payment.getRiskScore(),
                "ALLOW",
                payment.getDecisionReason() == null ? "MVP baseline risk decision" : payment.getDecisionReason(),
                "MVP_BASELINE_RULE"
        ));

        ledgerEntryRepository.save(new LedgerEntry(
                payment.getId(),
                payment.getPaymentId(),
                "PAYMENT_HOLD",
                payment.getSourceAccount(),
                payment.getAmount(),
                payment.getCurrency(),
                "DEBIT",
                "PENDING"
        ));

        ledgerEntryRepository.save(new LedgerEntry(
                payment.getId(),
                payment.getPaymentId(),
                "PAYMENT_RECEIVABLE",
                payment.getDestinationAccount(),
                payment.getAmount(),
                payment.getCurrency(),
                "CREDIT",
                "PENDING"
        ));
    }
}
