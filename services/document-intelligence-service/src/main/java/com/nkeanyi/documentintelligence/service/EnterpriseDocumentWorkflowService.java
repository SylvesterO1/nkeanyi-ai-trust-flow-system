package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.entity.DocumentAuditEvent;
import com.nkeanyi.documentintelligence.entity.DocumentFeedbackEvent;
import com.nkeanyi.documentintelligence.entity.DocumentReviewTask;
import com.nkeanyi.documentintelligence.entity.DocumentWorkflowState;
import com.nkeanyi.documentintelligence.model.DocumentAuditEventResponse;
import com.nkeanyi.documentintelligence.model.DocumentAuditEventsResponse;
import com.nkeanyi.documentintelligence.model.DocumentFeedbackRequest;
import com.nkeanyi.documentintelligence.model.DocumentFeedbackResponse;
import com.nkeanyi.documentintelligence.model.DocumentLifecycleState;
import com.nkeanyi.documentintelligence.model.DocumentOperationsMetricsResponse;
import com.nkeanyi.documentintelligence.model.DocumentReviewRequest;
import com.nkeanyi.documentintelligence.model.DocumentReviewTaskResponse;
import com.nkeanyi.documentintelligence.model.DocumentReviewTasksResponse;
import com.nkeanyi.documentintelligence.model.DocumentWorkflowResponse;
import com.nkeanyi.documentintelligence.model.ReviewDecision;
import com.nkeanyi.documentintelligence.model.ReviewTaskStatus;
import com.nkeanyi.documentintelligence.repository.DocumentAuditEventRepository;
import com.nkeanyi.documentintelligence.repository.DocumentFeedbackEventRepository;
import com.nkeanyi.documentintelligence.repository.DocumentReviewTaskRepository;
import com.nkeanyi.documentintelligence.repository.DocumentWorkflowStateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class EnterpriseDocumentWorkflowService {

    private static final String SYSTEM_ACTOR = "system";

    private final DocumentWorkflowStateRepository workflowRepository;
    private final DocumentReviewTaskRepository reviewTaskRepository;
    private final DocumentFeedbackEventRepository feedbackRepository;
    private final DocumentAuditEventRepository auditRepository;
    private final DocumentPolicyService policyService;
    private final ConcurrentMap<String, DocumentWorkflowState> workflowByDocumentId = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, DocumentReviewTask> reviewTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, DocumentFeedbackEvent> feedbackEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, DocumentAuditEvent> auditEvents = new ConcurrentHashMap<>();

    public EnterpriseDocumentWorkflowService(
            DocumentWorkflowStateRepository workflowRepository,
            DocumentReviewTaskRepository reviewTaskRepository,
            DocumentFeedbackEventRepository feedbackRepository,
            DocumentAuditEventRepository auditRepository,
            DocumentPolicyService policyService
    ) {
        this.workflowRepository = workflowRepository;
        this.reviewTaskRepository = reviewTaskRepository;
        this.feedbackRepository = feedbackRepository;
        this.auditRepository = auditRepository;
        this.policyService = policyService;
    }

    public EnterpriseDocumentWorkflowService() {
        this(null, null, null, null, new DocumentPolicyService());
    }

    public DocumentWorkflowResponse registerProcessedDocument(RegisterProcessedDocumentCommand command) {
        Optional<DocumentWorkflowState> existing = findWorkflow(command.documentId());
        if (existing.isPresent()) {
            return toWorkflowResponse(existing.get());
        }

        DocumentPolicyService.PolicyAssessment assessment = policyService.assess(
                command.originalFilename(),
                command.contentType(),
                command.extractedText(),
                command.confidence()
        );
        Instant now = Instant.now();
        String findings = joinFindings(assessment.findings());
        String lifecycleState;
        String reviewStatus;
        String quarantineReason = null;
        Instant publishedAt = null;

        if (assessment.quarantine()) {
            lifecycleState = DocumentLifecycleState.QUARANTINED.name();
            reviewStatus = ReviewTaskStatus.OPEN.name();
            quarantineReason = findings;
        } else if (assessment.reviewRequired()) {
            lifecycleState = DocumentLifecycleState.IN_REVIEW.name();
            reviewStatus = ReviewTaskStatus.OPEN.name();
        } else {
            lifecycleState = DocumentLifecycleState.PUBLISHED.name();
            reviewStatus = ReviewTaskStatus.AUTO_APPROVED.name();
            publishedAt = now;
        }

        DocumentWorkflowState state = new DocumentWorkflowState(
                command.documentId(),
                command.tenantId(),
                defaultIfBlank(command.workspaceId(), "default-workspace"),
                command.correlationId(),
                command.sourceUri(),
                command.checksum(),
                defaultIfBlank(command.ownerId(), "unassigned-owner"),
                command.originalFilename(),
                command.contentType(),
                command.documentType(),
                assessment.sensitivityLabel(),
                accessPolicyFor(assessment.sensitivityLabel()),
                lifecycleState,
                reviewStatus,
                quarantineReason,
                defaultIfBlank(command.retentionPolicy(), "standard-7-years"),
                assessment.qualityGateStatus(),
                findings,
                command.confidence(),
                command.ocrUsed(),
                command.extractorUsed(),
                publishedAt,
                now
        );

        saveWorkflow(state);
        audit(command.tenantId(), command.documentId(), SYSTEM_ACTOR, "DOCUMENT_RECEIVED", "SUCCESS", "Document registered", command.correlationId());
        audit(command.tenantId(), command.documentId(), SYSTEM_ACTOR, "DOCUMENT_CLASSIFIED", "SUCCESS", assessment.sensitivityLabel(), command.correlationId());
        audit(command.tenantId(), command.documentId(), SYSTEM_ACTOR, "DOCUMENT_PARSED", "SUCCESS", command.extractorUsed(), command.correlationId());
        audit(command.tenantId(), command.documentId(), SYSTEM_ACTOR, "DOCUMENT_ENRICHED", "SUCCESS", command.documentType(), command.correlationId());

        if (DocumentLifecycleState.PUBLISHED.name().equals(lifecycleState)) {
            audit(command.tenantId(), command.documentId(), SYSTEM_ACTOR, "DOCUMENT_PUBLISHED", "SUCCESS", "Auto-approved low-risk document", command.correlationId());
        } else {
            createReviewTask(command.documentId(), command.tenantId(), reviewReason(state), assessment.riskLevel(), "knowledge-curator");
        }

        return toWorkflowResponse(state);
    }

    public DocumentWorkflowResponse registerFailedDocument(RegisterFailedDocumentCommand command) {
        Optional<DocumentWorkflowState> existing = findWorkflow(command.documentId());
        if (existing.isPresent()) {
            return toWorkflowResponse(existing.get());
        }

        DocumentWorkflowState state = new DocumentWorkflowState(
                command.documentId(),
                command.tenantId(),
                defaultIfBlank(command.workspaceId(), "default-workspace"),
                command.correlationId(),
                command.sourceUri(),
                command.checksum(),
                defaultIfBlank(command.ownerId(), "unassigned-owner"),
                command.originalFilename(),
                command.contentType(),
                "UNKNOWN",
                "UNKNOWN",
                "TENANT_ONLY",
                DocumentLifecycleState.QUARANTINED.name(),
                ReviewTaskStatus.OPEN.name(),
                command.errorMessage(),
                defaultIfBlank(command.retentionPolicy(), "standard-7-years"),
                "FAILED",
                command.errorMessage(),
                null,
                null,
                null,
                null,
                Instant.now()
        );
        saveWorkflow(state);
        audit(command.tenantId(), command.documentId(), SYSTEM_ACTOR, "DOCUMENT_FAILED", "FAILURE", command.errorMessage(), command.correlationId());
        createReviewTask(command.documentId(), command.tenantId(), "Processing failure requires remediation", "HIGH", "document-analyst");
        return toWorkflowResponse(state);
    }

    public Optional<DocumentWorkflowResponse> findWorkflowResponse(String documentId) {
        return findWorkflow(documentId).map(this::toWorkflowResponse);
    }

    public DocumentWorkflowResponse reviewDocument(String documentId, DocumentReviewRequest request) {
        DocumentWorkflowState state = requireWorkflow(documentId);
        ReviewDecision decision = parseDecision(request.decision());
        String rationale = defaultIfBlank(request.rationale(), "No rationale supplied");
        state.applyReview(request.reviewerId(), decision.name(), rationale, request.ownerId(), request.retentionPolicy());

        switch (decision) {
            case APPROVE -> state.transition(DocumentLifecycleState.APPROVED.name(), ReviewTaskStatus.RESOLVED.name(), null);
            case REJECT, QUARANTINE -> state.transition(DocumentLifecycleState.QUARANTINED.name(), ReviewTaskStatus.RESOLVED.name(), rationale);
            case DEPRECATE -> state.deprecate(request.reviewerId(), request.replacementDocumentId(), rationale);
            case REDACT, MERGE, SPLIT -> state.transition(DocumentLifecycleState.IN_REVIEW.name(), ReviewTaskStatus.OPEN.name(), rationale);
        }

        saveWorkflow(state);
        resolveOpenTasks(documentId, request.reviewerId(), decision.name(), rationale);
        audit(state.tenantId(), documentId, request.reviewerId(), "DOCUMENT_REVIEWED", "SUCCESS", decision.name() + ": " + rationale, state.correlationId());
        return toWorkflowResponse(state);
    }

    public DocumentWorkflowResponse publishDocument(String documentId, String actorId) {
        DocumentWorkflowState state = requireWorkflow(documentId);
        if (DocumentLifecycleState.QUARANTINED.name().equals(state.lifecycleState())
                || DocumentLifecycleState.FAILED.name().equals(state.lifecycleState())) {
            audit(state.tenantId(), documentId, defaultIfBlank(actorId, SYSTEM_ACTOR), "DOCUMENT_PUBLISH_BLOCKED", "BLOCKED", state.quarantineReason(), state.correlationId());
            throw new IllegalStateException("Cannot publish quarantined or failed document: " + documentId);
        }

        state.publish(defaultIfBlank(actorId, SYSTEM_ACTOR));
        saveWorkflow(state);
        audit(state.tenantId(), documentId, defaultIfBlank(actorId, SYSTEM_ACTOR), "DOCUMENT_PUBLISHED", "SUCCESS", "Published to governed knowledge base", state.correlationId());
        return toWorkflowResponse(state);
    }

    public DocumentFeedbackResponse recordFeedback(String documentId, DocumentFeedbackRequest request) {
        DocumentWorkflowState state = requireWorkflow(documentId);
        DocumentFeedbackEvent event = new DocumentFeedbackEvent(
                documentId,
                state.tenantId(),
                request.userId(),
                request.category(),
                defaultIfBlank(request.severity(), "MEDIUM"),
                request.message()
        );
        saveFeedback(event);
        createReviewTask(documentId, state.tenantId(), "Feedback: " + request.category(), defaultIfBlank(request.severity(), "MEDIUM"), "knowledge-curator");
        audit(state.tenantId(), documentId, request.userId(), "DOCUMENT_FEEDBACK_RECORDED", "SUCCESS", request.category(), state.correlationId());
        return toFeedbackResponse(event);
    }

    public DocumentReviewTasksResponse listReviewTasks(String tenantId, String status, String riskLevel, int limit) {
        List<DocumentReviewTask> tasks = loadReviewTasks(tenantId, safeLimit(limit));
        List<DocumentReviewTaskResponse> responses = tasks.stream()
                .filter(task -> !hasText(status) || status.equalsIgnoreCase(task.status()))
                .filter(task -> !hasText(riskLevel) || riskLevel.equalsIgnoreCase(task.riskLevel()))
                .sorted(Comparator.comparing(DocumentReviewTask::createdAt).reversed())
                .limit(safeLimit(limit))
                .map(this::toReviewTaskResponse)
                .toList();
        return new DocumentReviewTasksResponse(responses, responses.size());
    }

    public DocumentAuditEventsResponse searchAuditEvents(
            String tenantId,
            String documentId,
            String userId,
            String action,
            Instant from,
            Instant to,
            int limit
    ) {
        List<DocumentAuditEventResponse> responses = loadAuditEvents(tenantId, safeLimit(limit)).stream()
                .filter(event -> !hasText(documentId) || documentId.equals(event.documentId()))
                .filter(event -> !hasText(userId) || userId.equals(event.actorId()))
                .filter(event -> !hasText(action) || action.equalsIgnoreCase(event.action()))
                .filter(event -> from == null || !event.timestamp().isBefore(from))
                .filter(event -> to == null || !event.timestamp().isAfter(to))
                .sorted(Comparator.comparing(DocumentAuditEvent::timestamp).reversed())
                .limit(safeLimit(limit))
                .map(this::toAuditResponse)
                .toList();
        return new DocumentAuditEventsResponse(responses, responses.size());
    }

    public DocumentOperationsMetricsResponse metrics() {
        List<DocumentWorkflowState> workflows = loadWorkflows(null, 1000);
        List<DocumentReviewTask> tasks = loadReviewTasks(null, 1000);
        Map<String, Long> byLifecycle = new LinkedHashMap<>();
        for (DocumentLifecycleState state : DocumentLifecycleState.values()) {
            long count = workflows.stream().filter(workflow -> state.name().equals(workflow.lifecycleState())).count();
            if (count > 0) {
                byLifecycle.put(state.name(), count);
            }
        }

        Map<String, Long> byTaskStatus = new LinkedHashMap<>();
        for (ReviewTaskStatus status : ReviewTaskStatus.values()) {
            long count = tasks.stream().filter(task -> status.name().equals(task.status())).count();
            if (count > 0) {
                byTaskStatus.put(status.name(), count);
            }
        }

        return new DocumentOperationsMetricsResponse(
                workflows.size(),
                tasks.stream().filter(task -> ReviewTaskStatus.OPEN.name().equals(task.status())).count(),
                workflows.stream().filter(workflow -> DocumentLifecycleState.QUARANTINED.name().equals(workflow.lifecycleState())).count(),
                feedbackCount(),
                byLifecycle,
                byTaskStatus
        );
    }

    private DocumentWorkflowState requireWorkflow(String documentId) {
        return findWorkflow(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document workflow not found: " + documentId));
    }

    private Optional<DocumentWorkflowState> findWorkflow(String documentId) {
        if (workflowRepository != null) {
            return workflowRepository.findByDocumentId(documentId);
        }
        return Optional.ofNullable(workflowByDocumentId.get(documentId));
    }

    private void saveWorkflow(DocumentWorkflowState state) {
        workflowByDocumentId.put(state.documentId(), state);
        if (workflowRepository != null) {
            workflowRepository.save(state);
        }
    }

    private void createReviewTask(String documentId, String tenantId, String reason, String riskLevel, String assignedRole) {
        DocumentReviewTask task = new DocumentReviewTask(documentId, tenantId, reason, riskLevel, assignedRole);
        reviewTasks.put(task.id(), task);
        if (reviewTaskRepository != null) {
            reviewTaskRepository.save(task);
        }
        audit(tenantId, documentId, SYSTEM_ACTOR, "REVIEW_TASK_CREATED", "SUCCESS", reason, null);
    }

    private void resolveOpenTasks(String documentId, String reviewerId, String decision, String rationale) {
        List<DocumentReviewTask> tasks = reviewTaskRepository != null
                ? reviewTaskRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                : reviewTasks.values().stream().filter(task -> documentId.equals(task.documentId())).toList();

        tasks.stream()
                .filter(task -> ReviewTaskStatus.OPEN.name().equals(task.status()))
                .forEach(task -> {
                    task.resolve(reviewerId, decision, rationale);
                    reviewTasks.put(task.id(), task);
                    if (reviewTaskRepository != null) {
                        reviewTaskRepository.save(task);
                    }
                });
    }

    private void saveFeedback(DocumentFeedbackEvent event) {
        feedbackEvents.put(event.id(), event);
        if (feedbackRepository != null) {
            feedbackRepository.save(event);
        }
    }

    private void audit(String tenantId, String documentId, String actorId, String action, String result, String reason, String correlationId) {
        DocumentAuditEvent event = new DocumentAuditEvent(
                defaultIfBlank(tenantId, "unknown-tenant"),
                documentId,
                defaultIfBlank(actorId, SYSTEM_ACTOR),
                action,
                result,
                reason,
                correlationId
        );
        auditEvents.put(event.id(), event);
        if (auditRepository != null) {
            auditRepository.save(event);
        }
    }

    private List<DocumentWorkflowState> loadWorkflows(String tenantId, int limit) {
        if (workflowRepository != null) {
            PageRequest page = PageRequest.of(0, safeLimit(limit));
            return hasText(tenantId)
                    ? workflowRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, page)
                    : workflowRepository.findAllByOrderByCreatedAtDesc(page);
        }
        return workflowByDocumentId.values().stream()
                .filter(workflow -> !hasText(tenantId) || tenantId.equals(workflow.tenantId()))
                .sorted(Comparator.comparing(DocumentWorkflowState::createdAt).reversed())
                .limit(safeLimit(limit))
                .toList();
    }

    private List<DocumentReviewTask> loadReviewTasks(String tenantId, int limit) {
        if (reviewTaskRepository != null) {
            PageRequest page = PageRequest.of(0, safeLimit(limit));
            return hasText(tenantId)
                    ? reviewTaskRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, page)
                    : reviewTaskRepository.findAllByOrderByCreatedAtDesc(page);
        }
        return reviewTasks.values().stream()
                .filter(task -> !hasText(tenantId) || tenantId.equals(task.tenantId()))
                .sorted(Comparator.comparing(DocumentReviewTask::createdAt).reversed())
                .limit(safeLimit(limit))
                .toList();
    }

    private List<DocumentAuditEvent> loadAuditEvents(String tenantId, int limit) {
        if (auditRepository != null) {
            PageRequest page = PageRequest.of(0, safeLimit(limit));
            return hasText(tenantId)
                    ? auditRepository.findByTenantIdOrderByTimestampDesc(tenantId, page)
                    : auditRepository.findAllByOrderByTimestampDesc(page);
        }
        return auditEvents.values().stream()
                .filter(event -> !hasText(tenantId) || tenantId.equals(event.tenantId()))
                .sorted(Comparator.comparing(DocumentAuditEvent::timestamp).reversed())
                .limit(safeLimit(limit))
                .toList();
    }

    private long feedbackCount() {
        if (feedbackRepository != null) {
            return feedbackRepository.count();
        }
        return feedbackEvents.size();
    }

    private DocumentWorkflowResponse toWorkflowResponse(DocumentWorkflowState state) {
        return new DocumentWorkflowResponse(
                state.documentId(),
                state.tenantId(),
                state.workspaceId(),
                state.correlationId(),
                state.sourceUri(),
                state.checksum(),
                state.ownerId(),
                state.originalFilename(),
                state.contentType(),
                state.documentType(),
                state.sensitivityLabel(),
                state.accessPolicy(),
                state.lifecycleState(),
                state.reviewStatus(),
                state.reviewerId(),
                state.reviewDecision(),
                state.decisionRationale(),
                state.quarantineReason(),
                state.replacementDocumentId(),
                state.retentionPolicy(),
                state.qualityGateStatus(),
                splitFindings(state.qualityGateFindings()),
                state.extractionConfidence(),
                state.ocrUsed(),
                state.extractorUsed(),
                state.publishedAt(),
                state.deprecatedAt(),
                state.createdAt(),
                state.updatedAt()
        );
    }

    private DocumentReviewTaskResponse toReviewTaskResponse(DocumentReviewTask task) {
        return new DocumentReviewTaskResponse(
                task.id(),
                task.documentId(),
                task.tenantId(),
                task.status(),
                task.reason(),
                task.riskLevel(),
                task.assignedRole(),
                task.reviewerId(),
                task.decision(),
                task.rationale(),
                task.createdAt(),
                task.resolvedAt()
        );
    }

    private DocumentFeedbackResponse toFeedbackResponse(DocumentFeedbackEvent event) {
        return new DocumentFeedbackResponse(
                event.id(),
                event.documentId(),
                event.tenantId(),
                event.userId(),
                event.category(),
                event.severity(),
                event.message(),
                event.status(),
                event.createdAt()
        );
    }

    private DocumentAuditEventResponse toAuditResponse(DocumentAuditEvent event) {
        return new DocumentAuditEventResponse(
                event.id(),
                event.tenantId(),
                event.documentId(),
                event.actorId(),
                event.action(),
                event.result(),
                event.reason(),
                event.correlationId(),
                event.timestamp()
        );
    }

    private ReviewDecision parseDecision(String decision) {
        return ReviewDecision.valueOf(decision.trim().toUpperCase(Locale.ROOT));
    }

    private String reviewReason(DocumentWorkflowState state) {
        if (DocumentLifecycleState.QUARANTINED.name().equals(state.lifecycleState())) {
            return defaultIfBlank(state.quarantineReason(), "Document quarantined by enterprise quality gates");
        }
        return defaultIfBlank(state.qualityGateFindings(), "Document requires curator approval before publishing");
    }

    private String accessPolicyFor(String sensitivityLabel) {
        return switch (sensitivityLabel == null ? "" : sensitivityLabel) {
            case "PUBLIC" -> "PUBLIC";
            case "RESTRICTED" -> "NAMED_REVIEWERS_ONLY";
            case "CONFIDENTIAL" -> "TENANT_CURATORS_ONLY";
            default -> "TENANT_ONLY";
        };
    }

    private String joinFindings(List<String> findings) {
        if (findings == null || findings.isEmpty()) {
            return "";
        }
        return String.join(" | ", findings);
    }

    private List<String> splitFindings(String findings) {
        if (!hasText(findings)) {
            return List.of();
        }
        return List.of(findings.split("\\s*\\|\\s*"));
    }

    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 25;
        }
        return Math.min(limit, 100);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record RegisterProcessedDocumentCommand(
            String documentId,
            String tenantId,
            String workspaceId,
            String correlationId,
            String sourceUri,
            String checksum,
            String ownerId,
            String originalFilename,
            String contentType,
            String documentType,
            String extractorUsed,
            Boolean ocrUsed,
            Double confidence,
            String extractedText,
            String retentionPolicy
    ) {}

    public record RegisterFailedDocumentCommand(
            String documentId,
            String tenantId,
            String workspaceId,
            String correlationId,
            String sourceUri,
            String checksum,
            String ownerId,
            String originalFilename,
            String contentType,
            String errorMessage,
            String retentionPolicy
    ) {}
}
