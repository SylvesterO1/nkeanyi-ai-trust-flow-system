package com.nkeanyi.payment.integration;

import com.nkeanyi.payment.entity.OutboxEvent;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.repository.OutboxEventRepository;
import com.nkeanyi.payment.repository.PaymentRepository;
import com.nkeanyi.payment.service.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OutboxPublisherIntegrationTest {

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

        // keep listener startup off so publishing can be tested in isolation
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.task.scheduling.enabled", () -> "false");
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void publishPendingEvents_shouldMarkOutboxEventAsPublished() {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setIdempotencyKey("outbox-itest-001");
        payment.setCustomerId("CUST-9001");
        payment.setSourceAccount("1234567890");
        payment.setDestinationAccount("0987654321");
        payment.setAmount(new BigDecimal("2750.00"));
        payment.setCurrency("USD");
        payment.setPaymentReference("INV-OUTBOX-001");
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setNarration("Outbox publisher integration test");
        payment.setStatus(PaymentStatus.RECEIVED);

        Payment savedPayment = paymentRepository.save(payment);

        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setAggregateType("PAYMENT");
        event.setAggregateId(savedPayment.getPaymentId());
        event.setTopic("payment.received");
        event.setStatus("NEW");
        event.setCreatedAt(LocalDateTime.now());
        event.setPayload("""
                {
                  "paymentId":"%s",
                  "customerId":"%s",
                  "sourceAccount":"%s",
                  "destinationAccount":"%s",
                  "amount":2750.00,
                  "currency":"USD",
                  "paymentReference":"INV-OUTBOX-001",
                  "paymentMethod":"BANK_TRANSFER",
                  "narration":"Outbox publisher integration test"
                }
                """.formatted(
                savedPayment.getPaymentId(),
                savedPayment.getCustomerId(),
                savedPayment.getSourceAccount(),
                savedPayment.getDestinationAccount()
        ));

        OutboxEvent savedEvent = outboxEventRepository.save(event);

        assertThat(savedEvent.getStatus()).isEqualTo("NEW");
        assertThat(savedEvent.getPublishedAt()).isNull();

        outboxPublisher.publishPendingEvents();

        Optional<OutboxEvent> updated = outboxEventRepository.findById(savedEvent.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo("PUBLISHED");
        assertThat(updated.get().getPublishedAt()).isNotNull();
    }
}
