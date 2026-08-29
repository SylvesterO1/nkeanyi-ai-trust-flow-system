# Function Catalog

Project root on disk:

- `/Users/a47487/payment-orchestration-service`

Primary source code location:

- `/Users/a47487/payment-orchestration-service/src/main/java`

## `src/main/java/com/nkeanyi/payment/PaymentOrchestrationApplication.java`

- `public static void main(String[] args)`

## `src/main/java/com/nkeanyi/payment/ai/AiDecisionRecord.java`

- `public boolean hasConfidenceScore()`
- `public boolean isApproved()`
- `private static String requireText(String value, String fieldName)`

## `src/main/java/com/nkeanyi/payment/api/GlobalExceptionHandler.java`

- `public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request)`
- `public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request)`
- `public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request)`
- `public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request)`
- `public ProblemDetail handleInvalidToken(InvalidBearerTokenException ex, HttpServletRequest request)`
- `public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request)`
- `public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request)`
- `private void enrich(ProblemDetail pd, String title, HttpServletRequest request, String code)`

## `src/main/java/com/nkeanyi/payment/audit/LoggingAuditService.java`

- `public void record(AuditEvent event)`

## `src/main/java/com/nkeanyi/payment/config/KafkaErrorHandlerConfig.java`

- `public ProducerFactory<Object, Object> dltProducerFactory(KafkaProperties kafkaProperties)`
- `public KafkaOperations<Object, Object> dltKafkaTemplate(ProducerFactory<Object, Object> dltProducerFactory)`
- `public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> dltKafkaTemplate)`

## `src/main/java/com/nkeanyi/payment/config/KafkaProducerConfig.java`

- `public KafkaTemplate<String, PaymentEvent> kafkaTemplate(ProducerFactory<String, PaymentEvent> producerFactory)`

## `src/main/java/com/nkeanyi/payment/config/OpenApiConfig.java`

- `public OpenAPI paymentOrchestrationOpenAPI()`

## `src/main/java/com/nkeanyi/payment/config/SecurityConfig.java`

- `public JwtAuthenticationConverter jwtAuthenticationConverter()`

## `src/main/java/com/nkeanyi/payment/consumer/PaymentDltConsumer.java`

- `public PaymentDltConsumer(PaymentRepository paymentRepository)`
- `public void handleDlt(PaymentEvent event)`

## `src/main/java/com/nkeanyi/payment/controller/PaymentController.java`

- `public PaymentController(PaymentService paymentService)`
- `public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id)`
- `public ResponseEntity<List<PaymentResponse>> getAllPayments()`

## `src/main/java/com/nkeanyi/payment/dto/CreatePaymentRequest.java`

- `public CreatePaymentRequest()`
- `public String getCustomerId()`
- `public void setCustomerId(String customerId)`
- `public String getSourceAccount()`
- `public void setSourceAccount(String sourceAccount)`
- `public String getDestinationAccount()`
- `public void setDestinationAccount(String destinationAccount)`
- `public BigDecimal getAmount()`
- `public void setAmount(BigDecimal amount)`
- `public String getCurrency()`
- `public void setCurrency(String currency)`
- `public String getPaymentReference()`
- `public void setPaymentReference(String paymentReference)`
- `public String getPaymentMethod()`
- `public void setPaymentMethod(String paymentMethod)`
- `public String getNarration()`
- `public void setNarration(String narration)`

## `src/main/java/com/nkeanyi/payment/dto/OrchestrationDecision.java`

- `public OrchestrationDecision()`
- `public OrchestrationDecision(boolean approved, String stage, String reason, Double riskScore)`
- `public boolean isApproved()`
- `public void setApproved(boolean approved)`
- `public String getStage()`
- `public void setStage(String stage)`
- `public String getReason()`
- `public void setReason(String reason)`
- `public Double getRiskScore()`
- `public void setRiskScore(Double riskScore)`

## `src/main/java/com/nkeanyi/payment/dto/PaymentCreateResult.java`

- `public PaymentCreateResult(PaymentResponse payment, boolean newlyCreated)`
- `public PaymentResponse getPayment()`
- `public boolean isNewlyCreated()`

## `src/main/java/com/nkeanyi/payment/dto/PaymentResponse.java`

- `public PaymentResponse()`
- `public Long getId()`
- `public void setId(Long id)`
- `public String getPaymentId()`
- `public void setPaymentId(String paymentId)`
- `public String getCustomerId()`
- `public void setCustomerId(String customerId)`
- `public String getSourceAccount()`
- `public void setSourceAccount(String sourceAccount)`
- `public String getDestinationAccount()`
- `public void setDestinationAccount(String destinationAccount)`
- `public BigDecimal getAmount()`
- `public void setAmount(BigDecimal amount)`
- `public String getCurrency()`
- `public void setCurrency(String currency)`
- `public String getPaymentReference()`
- `public void setPaymentReference(String paymentReference)`
- `public String getPaymentMethod()`
- `public void setPaymentMethod(String paymentMethod)`
- `public String getNarration()`
- `public void setNarration(String narration)`
- `public PaymentStatus getStatus()`
- `public void setStatus(PaymentStatus status)`
- `public Double getRiskScore()`
- `public void setRiskScore(Double riskScore)`
- `public String getDecisionReason()`
- `public void setDecisionReason(String decisionReason)`
- `public LocalDateTime getCreatedAt()`
- `public void setCreatedAt(LocalDateTime createdAt)`

## `src/main/java/com/nkeanyi/payment/entity/OutboxEvent.java`

- `public Long getId()`
- `public String getEventId()`
- `public void setEventId(String eventId)`
- `public String getAggregateType()`
- `public void setAggregateType(String aggregateType)`
- `public String getAggregateId()`
- `public void setAggregateId(String aggregateId)`
- `public String getTopic()`
- `public void setTopic(String topic)`
- `public String getPayload()`
- `public void setPayload(String payload)`
- `public String getStatus()`
- `public void setStatus(String status)`
- `public LocalDateTime getCreatedAt()`
- `public void setCreatedAt(LocalDateTime createdAt)`
- `public LocalDateTime getPublishedAt()`
- `public void setPublishedAt(LocalDateTime publishedAt)`

## `src/main/java/com/nkeanyi/payment/entity/Payment.java`

- `public Payment()`
- `public void prePersist()`
- `public void preUpdate()`
- `public Long getId()`
- `public String getPaymentId()`
- `public void setPaymentId(String paymentId)`
- `public String getIdempotencyKey()`
- `public void setIdempotencyKey(String idempotencyKey)`
- `public String getCustomerId()`
- `public void setCustomerId(String customerId)`
- `public String getSourceAccount()`
- `public void setSourceAccount(String sourceAccount)`
- `public String getDestinationAccount()`
- `public void setDestinationAccount(String destinationAccount)`
- `public BigDecimal getAmount()`
- `public void setAmount(BigDecimal amount)`
- `public String getCurrency()`
- `public void setCurrency(String currency)`
- `public String getPaymentReference()`
- `public void setPaymentReference(String paymentReference)`
- `public String getPaymentMethod()`
- `public void setPaymentMethod(String paymentMethod)`
- `public String getNarration()`
- `public void setNarration(String narration)`
- `public PaymentStatus getStatus()`
- `public void setStatus(PaymentStatus status)`
- `public Double getRiskScore()`
- `public void setRiskScore(Double riskScore)`
- `public String getDecisionReason()`
- `public void setDecisionReason(String decisionReason)`
- `public LocalDateTime getCreatedAt()`
- `public LocalDateTime getUpdatedAt()`
- `public LocalDateTime getProcessedAt()`
- `public void setProcessedAt(LocalDateTime processedAt)`

## `src/main/java/com/nkeanyi/payment/event/DecisionEvent.java`

- `public DecisionEvent()`
- `public String getPaymentId()`
- `public void setPaymentId(String paymentId)`
- `public String getDecisionType()`
- `public void setDecisionType(String decisionType)`
- `public boolean isApproved()`
- `public void setApproved(boolean approved)`
- `public Double getRiskScore()`
- `public void setRiskScore(Double riskScore)`
- `public String getReason()`
- `public void setReason(String reason)`

## `src/main/java/com/nkeanyi/payment/event/PaymentEvent.java`

- `public PaymentEvent()`
- `public String getPaymentId()`
- `public void setPaymentId(String paymentId)`
- `public String getCustomerId()`
- `public void setCustomerId(String customerId)`
- `public String getSourceAccount()`
- `public void setSourceAccount(String sourceAccount)`
- `public String getDestinationAccount()`
- `public void setDestinationAccount(String destinationAccount)`
- `public BigDecimal getAmount()`
- `public void setAmount(BigDecimal amount)`
- `public String getCurrency()`
- `public void setCurrency(String currency)`
- `public String getPaymentReference()`
- `public void setPaymentReference(String paymentReference)`
- `public String getPaymentMethod()`
- `public void setPaymentMethod(String paymentMethod)`
- `public String getNarration()`
- `public void setNarration(String narration)`

## `src/main/java/com/nkeanyi/payment/exception/ResourceNotFoundException.java`

- `public ResourceNotFoundException(String message)`

## `src/main/java/com/nkeanyi/payment/messaging/KafkaTopics.java`

- `private KafkaTopics()`

## `src/main/java/com/nkeanyi/payment/messaging/PaymentEventConsumer.java`

- `public void handlePaymentReceived(PaymentEvent event)`
- `private CreatePaymentRequest toRequest(PaymentEvent event)`

## `src/main/java/com/nkeanyi/payment/messaging/PaymentEventMapper.java`

- `public PaymentEvent from(Payment payment, CreatePaymentRequest request)`

## `src/main/java/com/nkeanyi/payment/messaging/PaymentEventPublisher.java`

- `public PaymentEventPublisher(KafkaTemplate<String, PaymentEvent> kafkaTemplate)`
- `public void publish(String topic, PaymentEvent event)`

## `src/main/java/com/nkeanyi/payment/observability/PaymentMetricsService.java`

- `public PaymentMetricsService(MeterRegistry meterRegistry)`
- `public void incrementPaymentCreated()`
- `public void incrementPaymentReplay()`
- `public void incrementPaymentStatusTransition(String status)`
- `public void incrementOutboxPublished()`
- `public void incrementOutboxPublishFailed()`
- `private void preRegisterStatus(String status)`
- `private Counter buildStatusCounter(String status)`

## `src/main/java/com/nkeanyi/payment/observability/PaymentTracingService.java`

- `public PaymentTracingService(Tracer tracer)`
- `public <T> T inSpan(String spanName, Supplier<T> action)`
- `public void annotateCurrentSpan(String key, String value)`

## `src/main/java/com/nkeanyi/payment/security/DataMaskingUtil.java`

- `private DataMaskingUtil()`
- `public static String maskPan(String value)`
- `public static String maskAccount(String value)`
- `public static String maskEmail(String value)`

## `src/main/java/com/nkeanyi/payment/service/AnomalyDetectionService.java`

- `public AnomalyDetectionService(PaymentRepository paymentRepository, AuditService auditService)`
- `public OrchestrationDecision evaluate(Payment payment, CreatePaymentRequest request)`

## `src/main/java/com/nkeanyi/payment/service/ComplianceScreeningService.java`

- `public OrchestrationDecision screen(CreatePaymentRequest request)`

## `src/main/java/com/nkeanyi/payment/service/FraudScoringService.java`

- `public OrchestrationDecision evaluate(CreatePaymentRequest request)`

## `src/main/java/com/nkeanyi/payment/service/OutboxPublisher.java`

- `public void publishPendingEvents()`

## `src/main/java/com/nkeanyi/payment/service/OutboxService.java`

- `public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper)`
- `public void saveEvent(String aggregateType, String aggregateId, String topic, Object payload)`

## `src/main/java/com/nkeanyi/payment/service/PaymentOrchestrationServiceImpl.java`

- `public Payment orchestrate(Payment payment, CreatePaymentRequest request)`
- `private void updateStatus(Payment payment, PaymentStatus status, String reason, Double riskScore)`
- `private Payment reload(Long id)`
- `private boolean isTerminalStatus(PaymentStatus status)`
- `private double defaultRiskScore(Double riskScore)`
- `private void auditStage(Payment payment, String stage, String reason, Double riskScore)`

## `src/main/java/com/nkeanyi/payment/service/PaymentServiceImpl.java`

- `public PaymentCreateResult createPayment(String idempotencyKey, CreatePaymentRequest request)`
- `public PaymentResponse getPaymentById(Long id)`
- `public List<PaymentResponse> getAllPayments()`
- `private PaymentResponse mapToResponse(Payment payment)`

## `src/main/java/com/nkeanyi/payment/service/PaymentValidationService.java`

- `public void validate(CreatePaymentRequest request)`

## Interface and repository methods

### `src/main/java/com/nkeanyi/payment/audit/AuditService.java`

- `void record(AuditEvent event)`

### `src/main/java/com/nkeanyi/payment/service/PaymentService.java`

- `PaymentCreateResult createPayment(String idempotencyKey, CreatePaymentRequest request)`
- `PaymentResponse getPaymentById(Long id)`
- `List<PaymentResponse> getAllPayments()`

### `src/main/java/com/nkeanyi/payment/service/PaymentOrchestrationService.java`

- `Payment orchestrate(Payment payment, CreatePaymentRequest request)`

### `src/main/java/com/nkeanyi/payment/repository/PaymentRepository.java`

- `Optional<Payment> findByPaymentId(String paymentId)`
- `Optional<Payment> findByIdempotencyKey(String idempotencyKey)`
- `long countByCustomerIdAndCreatedAtAfter(String customerId, LocalDateTime cutoff)`
- `long countByDestinationAccountAndCreatedAtAfter(String destinationAccount, LocalDateTime cutoff)`
- `long countByCustomerIdAndAmountGreaterThanEqualAndCreatedAtAfter(String customerId, BigDecimal amount, LocalDateTime cutoff)`
- `int updateTerminalState(Long paymentId, PaymentStatus status, String reason, Double riskScore, LocalDateTime updatedAt, LocalDateTime processedAt)`
- `int updateNonTerminalState(Long paymentId, PaymentStatus status, String reason, Double riskScore, LocalDateTime updatedAt)`

### `src/main/java/com/nkeanyi/payment/repository/OutboxEventRepository.java`

- `List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status)`
- `Optional<OutboxEvent> findByEventId(String eventId)`

## Other storage locations on this computer

Built classes and jars:

- `/Users/a47487/payment-orchestration-service/target/classes`
- `/Users/a47487/payment-orchestration-service/target/payment-orchestration-service-1.0.0.jar`

Test reports:

- `/Users/a47487/payment-orchestration-service/target/surefire-reports`

Runtime data for this project when using Docker:

- Postgres container data is stored in the Docker volume `postgres_data`
- Grafana data is stored in the Docker volume `grafana_data`

Docker-managed volumes are stored inside Docker Desktop’s VM, not as normal project files in this repo.
