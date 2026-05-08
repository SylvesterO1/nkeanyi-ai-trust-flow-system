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
class PaymentFailureDltIntegrationTest {

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
    void failedConsumerEvent_shouldGoToDltAndMarkPaymentFailed() throws Exception {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setIdempotencyKey("dlt-itest-001");
        payment.setCustomerId("CUST-DLT-001");
        payment.setSourceAccount("1111222233");
        payment.setDestinationAccount("9999888877");
        payment.setAmount(new BigDecimal("5000.00"));
        payment.setCurrency("USD");
        payment.setPaymentReference("FORCE-DLT");
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setNarration("DLT failure path integration test");
        payment.setStatus(PaymentStatus.RECEIVED);

        Payment savedPayment = paymentRepository.save(payment);

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

        Payment updated = waitForFailedState(savedPayment.getId(), Duration.ofSeconds(20));

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updated.getDecisionReason()).isEqualTo("Moved to DLT after consumer failure");
    }

    private Payment waitForFailedState(Long paymentDbId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            Optional<Payment> current = paymentRepository.findById(paymentDbId);
            if (current.isPresent()) {
                Payment payment = current.get();
                if (payment.getStatus() == PaymentStatus.FAILED) {
                    return payment;
                }
            }
            Thread.sleep(500);
        }

        return paymentRepository.findById(paymentDbId)
                .orElseThrow(() -> new IllegalStateException("Payment not found after waiting"));
    }
}
