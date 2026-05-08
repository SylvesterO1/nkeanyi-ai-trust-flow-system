package com.nkeanyi.payment.integration;

import com.nkeanyi.payment.entity.OutboxEvent;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.repository.OutboxEventRepository;
import com.nkeanyi.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentApiIntegrationTest {

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

        // Avoid real JWT discovery in tests; jwt() from spring-security-test provides auth directly
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/fake-jwks");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void createPayment_shouldPersistPaymentAndOutboxEvent() throws Exception {
        String requestBody = """
                {
                  "customerId": "CUST-1001",
                  "sourceAccount": "1234567890",
                  "destinationAccount": "0987654321",
                  "amount": 2500.00,
                  "currency": "USD",
                  "paymentReference": "INV-TEST-001",
                  "paymentMethod": "BANK_TRANSFER",
                  "narration": "Vendor settlement"
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_payments.write")))
                        .header("Idempotency-Key", "itest-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("CUST-1001"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        List<Payment> payments = paymentRepository.findAll();
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();

        assertThat(payments).hasSize(1);
        assertThat(outboxEvents).hasSize(1);

        Payment payment = payments.get(0);
        assertThat(payment.getIdempotencyKey()).isEqualTo("itest-001");
        assertThat(payment.getStatus().name()).isEqualTo("RECEIVED");

        OutboxEvent outboxEvent = outboxEvents.get(0);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(payment.getPaymentId());
        assertThat(outboxEvent.getTopic()).isEqualTo("payment.received");
        assertThat(outboxEvent.getStatus()).isEqualTo("NEW");
    }

    @Test
    void createPayment_withSameIdempotencyKey_shouldReturnExistingPayment() throws Exception {
        String requestBody = """
                {
                  "customerId": "CUST-1001",
                  "sourceAccount": "1234567890",
                  "destinationAccount": "0987654321",
                  "amount": 2500.00,
                  "currency": "USD",
                  "paymentReference": "INV-TEST-002",
                  "paymentMethod": "BANK_TRANSFER",
                  "narration": "Vendor settlement"
                }
                """;

        String firstResponse = mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_payments.write")))
                        .header("Idempotency-Key", "itest-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_payments.write")))
                        .header("Idempotency-Key", "itest-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Payment> payments = paymentRepository.findAll();
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();

        assertThat(payments).hasSize(1);
        assertThat(outboxEvents).hasSize(1);
        assertThat(secondResponse).isEqualTo(firstResponse);
    }

    @Test
    void getPayment_shouldRequireReadScope() throws Exception {
        Payment payment = new Payment();
        payment.setPaymentId("pay-test-003");
        payment.setIdempotencyKey("itest-003");
        payment.setCustomerId("CUST-2001");
        payment.setSourceAccount("1234567890");
        payment.setDestinationAccount("0987654321");
        payment.setAmount(new java.math.BigDecimal("1500.00"));
        payment.setCurrency("USD");
        payment.setPaymentReference("INV-READ-001");
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setNarration("Read scope test");
        payment.setStatus(com.nkeanyi.payment.enums.PaymentStatus.RECEIVED);

        Payment saved = paymentRepository.save(payment);

        mockMvc.perform(get("/api/v1/payments/{id}", saved.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_payments.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.customerId").value("CUST-2001"));
    }

    @Test
    void postPayment_withReadOnlyScope_shouldBeForbidden() throws Exception {
        String requestBody = """
                {
                  "customerId": "CUST-3001",
                  "sourceAccount": "1234567890",
                  "destinationAccount": "0987654321",
                  "amount": 2000.00,
                  "currency": "USD",
                  "paymentReference": "INV-FORBIDDEN-001",
                  "paymentMethod": "BANK_TRANSFER",
                  "narration": "Forbidden scope test"
                }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_payments.read")))
                        .header("Idempotency-Key", "itest-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }
}
