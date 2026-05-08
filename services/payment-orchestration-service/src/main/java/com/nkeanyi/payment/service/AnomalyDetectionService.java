package com.nkeanyi.payment.service;

import com.nkeanyi.payment.ai.AiDecisionRecord;
import com.nkeanyi.payment.audit.AuditEvent;
import com.nkeanyi.payment.audit.AuditService;
import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.OrchestrationDecision;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnomalyDetectionService {

    private static final BigDecimal LARGE_PAYMENT_THRESHOLD = new BigDecimal("5000.00");
    private static final int HIGH_VELOCITY_THRESHOLD = 4;
    private static final int DESTINATION_CLUSTER_THRESHOLD = 4;
    private static final int LARGE_PAYMENT_PATTERN_THRESHOLD = 3;
    private static final double REJECTION_THRESHOLD = 70.0;

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public AnomalyDetectionService(PaymentRepository paymentRepository, AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    public OrchestrationDecision evaluate(Payment payment, CreatePaymentRequest request) {
        LocalDateTime tenMinuteCutoff = LocalDateTime.now().minusMinutes(10);
        LocalDateTime oneDayCutoff = LocalDateTime.now().minusHours(24);

        long recentCustomerPayments = paymentRepository.countByCustomerIdAndCreatedAtAfter(
                request.getCustomerId(), tenMinuteCutoff
        );
        long recentDestinationPayments = paymentRepository.countByDestinationAccountAndCreatedAtAfter(
                request.getDestinationAccount(), tenMinuteCutoff
        );
        long recentLargePayments = paymentRepository.countByCustomerIdAndAmountGreaterThanEqualAndCreatedAtAfter(
                request.getCustomerId(), LARGE_PAYMENT_THRESHOLD, oneDayCutoff
        );

        double score = 0.0;
        List<String> triggeredSignals = new ArrayList<>();

        if (recentCustomerPayments >= HIGH_VELOCITY_THRESHOLD) {
            score += 45.0;
            triggeredSignals.add("customer_velocity_spike");
        }

        if (recentDestinationPayments >= DESTINATION_CLUSTER_THRESHOLD) {
            score += 35.0;
            triggeredSignals.add("destination_account_cluster");
        }

        if (request.getAmount() != null
                && request.getAmount().compareTo(LARGE_PAYMENT_THRESHOLD) >= 0
                && recentLargePayments >= LARGE_PAYMENT_PATTERN_THRESHOLD) {
            score += 20.0;
            triggeredSignals.add("repeated_large_payments");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("paymentId", payment.getPaymentId());
        metadata.put("recentCustomerPayments10m", recentCustomerPayments);
        metadata.put("recentDestinationPayments10m", recentDestinationPayments);
        metadata.put("recentLargePayments24h", recentLargePayments);
        metadata.put("triggeredSignals", triggeredSignals);
        metadata.put("anomalyScore", score);

        boolean approved = score < REJECTION_THRESHOLD;
        String reason = approved
                ? "Anomaly screening passed"
                : "Payment flagged by anomaly detection";

        AiDecisionRecord aiDecisionRecord = new AiDecisionRecord(
                "payment-anomaly-detection",
                "rule-based-anomaly-detector",
                "1.0.0",
                "payment:" + payment.getPaymentId(),
                "decision:" + payment.getPaymentId(),
                Math.min(score / 100.0, 1.0),
                score >= 50.0,
                approved ? "APPROVED" : "REJECTED",
                Instant.now(),
                metadata
        );

        auditService.record(new AuditEvent(
                "AI_DECISION_RECORDED",
                payment.getCustomerId(),
                "PAYMENT",
                payment.getPaymentId(),
                aiDecisionRecord.finalDecision(),
                Instant.now(),
                Map.of("aiDecisionRecord", aiDecisionRecord)
        ));

        return new OrchestrationDecision(approved, "ANOMALY_REVIEW", reason, score);
    }
}
