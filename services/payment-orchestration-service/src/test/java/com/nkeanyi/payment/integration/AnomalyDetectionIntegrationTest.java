package com.nkeanyi.payment.integration;

import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AnomalyDetectionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("paymentdb")
            .withUsername("paymentuser")
            .withPassword("paymentpass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/fake-jwks");
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
        registry.add("spring.task.scheduling.enabled", () -> "false");
        registry.add("management.tracing.enabled", () -> "false");
        registry.add("management.prometheus.metrics.export.enabled", () -> "false");
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @BeforeEach
    void cleanUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void clusteredPaymentPattern_shouldBeRejectedByAnomalyDetection() throws Exception {
        String customerId = "CUST-ANOM-001";
        String destinationAccount = "7777888899";

        for (int i = 0; i < 3; i++) {
            paymentRepository.save(payment(
                    "anomaly-prior-" + i,
                    customerId,
                    "11112222" + i,
                    destinationAccount,
                    new BigDecimal("2200.00"),
                    "ANOMALY-PRIOR-" + i,
                    "Historical anomaly seed " + i
            ));
        }

        Payment current = payment(
                "anomaly-current",
                customerId,
                "2222333344",
                destinationAccount,
                new BigDecimal("2300.00"),
                "ANOMALY-CURRENT-001",
                "Current payment expected to trigger anomaly review"
        );
        Payment savedPayment = paymentRepository.save(current);

        PaymentEvent event = new PaymentEvent();
        event.setPaymentId(savedPayment.getPaymentId());
        event.setCustomerId(savedPayment.getCustomerId());
        event.setSourceAccount(savedPayment.getSourceAccount());
        event.setDestinationAccount(savedPayment.getDestinationAccount());
        event.setAmount(savedPayment.getAmount());
        event.setCurrency(savedPayment.getCurrency());
        event.setPaymentReference(savedPayment.getPaymentReference());
        event.setPaymentMethod(savedPayment.getPaymentMethod());
        event.setNarration(savedPayment.getNarration());

        kafkaTemplate.send("payment.received", savedPayment.getPaymentId(), event).get();

        Payment updated = waitForTerminalStatus(savedPayment.getId(), Duration.ofSeconds(15));

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(updated.getDecisionReason()).isEqualTo("Payment flagged by anomaly detection");
        assertThat(updated.getRiskScore()).isEqualTo(80.0);
    }

    private Payment payment(String idempotencyKey,
                            String customerId,
                            String sourceAccount,
                            String destinationAccount,
                            BigDecimal amount,
                            String paymentReference,
                            String narration) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCustomerId(customerId);
        payment.setSourceAccount(sourceAccount);
        payment.setDestinationAccount(destinationAccount);
        payment.setAmount(amount);
        payment.setCurrency("USD");
        payment.setPaymentReference(paymentReference);
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setNarration(narration);
        payment.setStatus(PaymentStatus.RECEIVED);
        return payment;
    }

    private Payment waitForTerminalStatus(Long paymentDbId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            Optional<Payment> current = paymentRepository.findById(paymentDbId);
            if (current.isPresent()) {
                Payment payment = current.get();
                if (payment.getStatus() == PaymentStatus.COMPLETED
                        || payment.getStatus() == PaymentStatus.REJECTED
                        || payment.getStatus() == PaymentStatus.FAILED) {
                    return payment;
                }
            }
            Thread.sleep(500);
        }

        return paymentRepository.findById(paymentDbId)
                .orElseThrow(() -> new IllegalStateException("Payment not found after waiting"));
    }
}
