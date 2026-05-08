package com.nkeanyi.payment.service;

import com.nkeanyi.payment.audit.AuditEvent;
import com.nkeanyi.payment.audit.AuditService;
import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.OrchestrationDecision;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectionServiceTest {

    @Test
    void shouldApproveNormalPaymentAndRecordAiDecision() {
        RecordingAuditService auditService = new RecordingAuditService();
        PaymentRepository paymentRepository = paymentRepository(1L, 1L, 1L);
        AnomalyDetectionService service = new AnomalyDetectionService(paymentRepository, auditService);

        OrchestrationDecision decision = service.evaluate(payment("pay-normal"), request(new BigDecimal("1500.00")));

        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getStage()).isEqualTo("ANOMALY_REVIEW");
        assertThat(decision.getReason()).isEqualTo("Anomaly screening passed");
        assertThat(decision.getRiskScore()).isEqualTo(0.0);
        assertThat(auditService.lastEvent).isNotNull();
        assertThat(auditService.lastEvent.outcome()).isEqualTo("APPROVED");
    }

    @Test
    void shouldRejectAnomalousPaymentBasedOnVelocityAndDestinationCluster() {
        RecordingAuditService auditService = new RecordingAuditService();
        PaymentRepository paymentRepository = paymentRepository(4L, 4L, 2L);
        AnomalyDetectionService service = new AnomalyDetectionService(paymentRepository, auditService);

        OrchestrationDecision decision = service.evaluate(payment("pay-anomalous"), request(new BigDecimal("2000.00")));

        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).isEqualTo("Payment flagged by anomaly detection");
        assertThat(decision.getRiskScore()).isEqualTo(80.0);
        assertThat(auditService.lastEvent).isNotNull();
        assertThat(auditService.lastEvent.outcome()).isEqualTo("REJECTED");
    }

    private PaymentRepository paymentRepository(long customerCount, long destinationCount, long largePaymentCount) {
        return (PaymentRepository) Proxy.newProxyInstance(
                PaymentRepository.class.getClassLoader(),
                new Class[]{PaymentRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "countByCustomerIdAndCreatedAtAfter" -> customerCount;
                    case "countByDestinationAccountAndCreatedAtAfter" -> destinationCount;
                    case "countByCustomerIdAndAmountGreaterThanEqualAndCreatedAtAfter" -> largePaymentCount;
                    case "findByPaymentId", "findByIdempotencyKey", "findById" -> Optional.empty();
                    case "count" -> 0L;
                    case "existsById" -> false;
                    case "save" -> args[0];
                    case "saveAll" -> args[0];
                    case "findAll" -> java.util.List.of();
                    case "findAllById" -> java.util.List.of();
                    case "flush", "deleteAllInBatch", "deleteAll", "deleteAllByIdInBatch", "deleteAllById",
                         "deleteInBatch", "delete", "deleteById" -> null;
                    case "getReferenceById", "getById", "getOne" -> null;
                    default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                }
        );
    }

    private Payment payment(String paymentId) {
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setCustomerId("CUST-1001");
        payment.setDestinationAccount("0987654321");
        return payment;
    }

    private CreatePaymentRequest request(BigDecimal amount) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setCustomerId("CUST-1001");
        request.setSourceAccount("1234567890");
        request.setDestinationAccount("0987654321");
        request.setAmount(amount);
        request.setCurrency("USD");
        request.setPaymentReference("REF-1001");
        request.setPaymentMethod("BANK_TRANSFER");
        request.setNarration("Normal enterprise payment");
        return request;
    }

    private static class RecordingAuditService implements AuditService {
        private AuditEvent lastEvent;

        @Override
        public void record(AuditEvent event) {
            this.lastEvent = event;
        }
    }
}
