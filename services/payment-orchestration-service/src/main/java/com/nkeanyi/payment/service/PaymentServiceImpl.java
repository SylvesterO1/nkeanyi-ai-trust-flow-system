package com.nkeanyi.payment.service;

import com.nkeanyi.payment.audit.AuditEvent;
import com.nkeanyi.payment.audit.AuditService;
import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.dto.PaymentCreateResult;
import com.nkeanyi.payment.dto.PaymentResponse;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.enums.PaymentStatus;
import com.nkeanyi.payment.exception.ResourceNotFoundException;
import com.nkeanyi.payment.messaging.KafkaTopics;
import com.nkeanyi.payment.messaging.PaymentEventMapper;
import com.nkeanyi.payment.observability.PaymentMetricsService;
import com.nkeanyi.payment.observability.PaymentTracingService;
import com.nkeanyi.payment.repository.PaymentRepository;
import com.nkeanyi.payment.security.DataMaskingUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;
    private final PaymentEventMapper paymentEventMapper;
    private final OutboxService outboxService;
    private final PaymentMetricsService paymentMetricsService;
    private final PaymentTracingService paymentTracingService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              AuditService auditService,
                              PaymentEventMapper paymentEventMapper,
                              OutboxService outboxService,
                              PaymentMetricsService paymentMetricsService,
                              PaymentTracingService paymentTracingService) {
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
        this.paymentEventMapper = paymentEventMapper;
        this.outboxService = outboxService;
        this.paymentMetricsService = paymentMetricsService;
        this.paymentTracingService = paymentTracingService;
    }

    @Override
    @Transactional
    public PaymentCreateResult createPayment(String idempotencyKey, CreatePaymentRequest request) {
        return paymentTracingService.inSpan("payment.create", () -> {
            paymentTracingService.annotateCurrentSpan("payment.idempotency_key", idempotencyKey);
            paymentTracingService.annotateCurrentSpan("payment.customer_id", request.getCustomerId());
            paymentTracingService.annotateCurrentSpan("payment.currency", request.getCurrency());

            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent()) {
                paymentMetricsService.incrementPaymentReplay();
                paymentTracingService.annotateCurrentSpan("payment.replayed", "true");
                return new PaymentCreateResult(mapToResponse(existingPayment.get()), false);
            }

            try {
                Payment payment = new Payment();
                payment.setPaymentId(UUID.randomUUID().toString());
                payment.setIdempotencyKey(idempotencyKey);
                payment.setCustomerId(request.getCustomerId());
                payment.setSourceAccount(request.getSourceAccount());
                payment.setDestinationAccount(request.getDestinationAccount());
                payment.setAmount(request.getAmount());
                payment.setCurrency(request.getCurrency());
                payment.setPaymentReference(request.getPaymentReference());
                payment.setPaymentMethod(request.getPaymentMethod());
                payment.setNarration(request.getNarration());
                payment.setStatus(PaymentStatus.RECEIVED);

                Payment savedPayment = paymentRepository.save(payment);
                paymentTracingService.annotateCurrentSpan("payment.payment_id", savedPayment.getPaymentId());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("amount", savedPayment.getAmount());
                metadata.put("currency", savedPayment.getCurrency());
                metadata.put("sourceAccount", DataMaskingUtil.maskAccount(savedPayment.getSourceAccount()));
                metadata.put("destinationAccount", DataMaskingUtil.maskAccount(savedPayment.getDestinationAccount()));
                metadata.put("paymentReference", savedPayment.getPaymentReference());
                metadata.put("idempotencyKey", savedPayment.getIdempotencyKey());

                auditService.record(new AuditEvent(
                        "PAYMENT_CREATED",
                        savedPayment.getCustomerId(),
                        "PAYMENT",
                        savedPayment.getPaymentId(),
                        "SUCCESS",
                        Instant.now(),
                        metadata
                ));

                outboxService.saveEvent(
                        "PAYMENT",
                        savedPayment.getPaymentId(),
                        KafkaTopics.PAYMENT_RECEIVED,
                        paymentEventMapper.from(savedPayment, request)
                );

                paymentMetricsService.incrementPaymentCreated();
                paymentTracingService.annotateCurrentSpan("payment.replayed", "false");

                return new PaymentCreateResult(mapToResponse(savedPayment), true);

            } catch (DataIntegrityViolationException ex) {
                Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> ex);
                paymentMetricsService.incrementPaymentReplay();
                paymentTracingService.annotateCurrentSpan("payment.replayed", "true");
                return new PaymentCreateResult(mapToResponse(existing), false);
            }
        });
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setPaymentId(payment.getPaymentId());
        response.setCustomerId(payment.getCustomerId());
        response.setSourceAccount(DataMaskingUtil.maskAccount(payment.getSourceAccount()));
        response.setDestinationAccount(DataMaskingUtil.maskAccount(payment.getDestinationAccount()));
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaymentReference(payment.getPaymentReference());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setNarration(payment.getNarration());
        response.setStatus(payment.getStatus());
        response.setRiskScore(payment.getRiskScore());
        response.setDecisionReason(payment.getDecisionReason());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
