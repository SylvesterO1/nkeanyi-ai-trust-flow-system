package com.nkeanyi.payment.service;

import com.nkeanyi.payment.audit.AuditEvent;
import com.nkeanyi.payment.audit.AuditService;
import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.OrchestrationDecision;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.observability.PaymentMetricsService;
import com.nkeanyi.payment.observability.PaymentTracingService;
import com.nkeanyi.payment.repository.PaymentRepository;
import com.nkeanyi.payment.security.DataMaskingUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class PaymentOrchestrationServiceImpl implements PaymentOrchestrationService {

    private final PaymentRepository paymentRepository;
    private final PaymentValidationService paymentValidationService;
    private final FraudScoringService fraudScoringService;
    private final ComplianceScreeningService complianceScreeningService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AuditService auditService;
    private final PaymentMetricsService paymentMetricsService;
    private final PaymentTracingService paymentTracingService;

    public PaymentOrchestrationServiceImpl(PaymentRepository paymentRepository,
                                           PaymentValidationService paymentValidationService,
                                           FraudScoringService fraudScoringService,
                                           ComplianceScreeningService complianceScreeningService,
                                           AnomalyDetectionService anomalyDetectionService,
                                           AuditService auditService,
                                           PaymentMetricsService paymentMetricsService,
                                           PaymentTracingService paymentTracingService) {
        this.paymentRepository = paymentRepository;
        this.paymentValidationService = paymentValidationService;
        this.fraudScoringService = fraudScoringService;
        this.complianceScreeningService = complianceScreeningService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.auditService = auditService;
        this.paymentMetricsService = paymentMetricsService;
        this.paymentTracingService = paymentTracingService;
    }

    @Override
    public Payment orchestrate(Payment payment, CreatePaymentRequest request) {
        return paymentTracingService.inSpan("payment.orchestrate", () -> {
            paymentTracingService.annotateCurrentSpan("payment.payment_id", payment.getPaymentId());
            paymentTracingService.annotateCurrentSpan("payment.customer_id", payment.getCustomerId());

            try {
                updateStatus(payment, PaymentStatus.VALIDATING, "Validation started", null);

                paymentValidationService.validate(request);

                updateStatus(payment, PaymentStatus.COMPLIANCE_REVIEW, "Compliance screening started", null);

                OrchestrationDecision complianceDecision = complianceScreeningService.screen(request);
                if (!complianceDecision.isApproved()) {
                    updateStatus(payment, PaymentStatus.REJECTED,
                            complianceDecision.getReason(), complianceDecision.getRiskScore());
                    return reload(payment.getId());
                }

                updateStatus(payment, PaymentStatus.ANOMALY_REVIEW, "Anomaly detection started", null);

                OrchestrationDecision anomalyDecision = anomalyDetectionService.evaluate(payment, request);
                if (!anomalyDecision.isApproved()) {
                    updateStatus(payment, PaymentStatus.REJECTED,
                            anomalyDecision.getReason(), anomalyDecision.getRiskScore());
                    return reload(payment.getId());
                }

                updateStatus(payment, PaymentStatus.FRAUD_REVIEW, "Fraud screening started", null);

                OrchestrationDecision fraudDecision = fraudScoringService.evaluate(request);
                if (!fraudDecision.isApproved()) {
                    updateStatus(payment, PaymentStatus.REJECTED,
                            fraudDecision.getReason(), fraudDecision.getRiskScore());
                    return reload(payment.getId());
                }

                double finalRiskScore = Math.max(
                        defaultRiskScore(anomalyDecision.getRiskScore()),
                        defaultRiskScore(fraudDecision.getRiskScore())
                );

                updateStatus(payment, PaymentStatus.APPROVED, "Payment approved", finalRiskScore);
                updateStatus(payment, PaymentStatus.PROCESSING, "Payment execution in progress", finalRiskScore);
                updateStatus(payment, PaymentStatus.COMPLETED, "Payment completed successfully", finalRiskScore);

                return reload(payment.getId());

            } catch (IllegalArgumentException ex) {
                updateStatus(payment, PaymentStatus.REJECTED, ex.getMessage(), null);
                throw ex;
            } catch (Exception ex) {
                updateStatus(payment, PaymentStatus.FAILED, ex.getMessage(), null);
                throw ex;
            }
        });
    }

    private void updateStatus(Payment payment, PaymentStatus status, String reason, Double riskScore) {
        paymentTracingService.inSpan("payment.status." + status.name().toLowerCase(), () -> {
            LocalDateTime now = LocalDateTime.now();

            if (isTerminalStatus(status)) {
                paymentRepository.updateTerminalState(
                        payment.getId(),
                        status,
                        reason,
                        riskScore,
                        now,
                        now
                );
            } else {
                paymentRepository.updateNonTerminalState(
                        payment.getId(),
                        status,
                        reason,
                        riskScore,
                        now
                );
            }

            Payment updated = reload(payment.getId());

            paymentMetricsService.incrementPaymentStatusTransition(status.name());
            paymentTracingService.annotateCurrentSpan("payment.status", status.name());
            auditStage(updated, status.name(), reason, riskScore);
            return null;
        });
    }

    private Payment reload(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Payment not found after status update: " + id));
    }

    private boolean isTerminalStatus(PaymentStatus status) {
        return status == PaymentStatus.COMPLETED
                || status == PaymentStatus.REJECTED
                || status == PaymentStatus.FAILED;
    }

    private double defaultRiskScore(Double riskScore) {
        return riskScore == null ? 0.0 : riskScore;
    }

    private void auditStage(Payment payment, String stage, String reason, Double riskScore) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", payment.getPaymentId());
        metadata.put("status", payment.getStatus().name());
        metadata.put("amount", payment.getAmount());
        metadata.put("currency", payment.getCurrency());
        metadata.put("sourceAccount", DataMaskingUtil.maskAccount(payment.getSourceAccount()));
        metadata.put("destinationAccount", DataMaskingUtil.maskAccount(payment.getDestinationAccount()));
        metadata.put("reason", reason);
        metadata.put("riskScore", riskScore);
        metadata.put("processedAt", payment.getProcessedAt());

        auditService.record(new AuditEvent(
                "PAYMENT_STAGE_UPDATE",
                payment.getCustomerId(),
                "PAYMENT",
                payment.getPaymentId(),
                stage,
                Instant.now(),
                metadata
        ));
    }
}
