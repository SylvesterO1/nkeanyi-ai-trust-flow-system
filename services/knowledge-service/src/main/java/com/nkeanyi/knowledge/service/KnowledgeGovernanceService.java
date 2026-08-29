package com.nkeanyi.knowledge.service;

import com.nkeanyi.knowledge.citation.Citation;
import com.nkeanyi.knowledge.citation.CitationPackage;
import com.nkeanyi.knowledge.model.KnowledgeEvaluationResponse;
import com.nkeanyi.knowledge.model.KnowledgeFeedbackRequest;
import com.nkeanyi.knowledge.model.KnowledgeFeedbackResponse;
import com.nkeanyi.knowledge.model.KnowledgeOperationsMetricsResponse;
import com.nkeanyi.knowledge.model.KnowledgeQueryRequest;
import com.nkeanyi.knowledge.model.RetrievalEventResponse;
import com.nkeanyi.knowledge.model.RetrievalEventsResponse;
import com.nkeanyi.knowledge.observability.RegulatedRequestContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Service
public class KnowledgeGovernanceService {

    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    private final ConcurrentMap<UUID, RetrievalEvent> retrievalEvents = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, FeedbackEvent> feedbackEvents = new ConcurrentHashMap<>();

    public PolicyDecision authorize(KnowledgeQueryRequest request) {
        if (request == null) {
            return PolicyDecision.allow();
        }

        if (containsPromptInjection(request.query())) {
            return PolicyDecision.block("PROMPT_INJECTION_QUERY", "Query contains instruction-override language.");
        }

        if (request.documentId() != null
                && request.authorizedDocumentIds() != null
                && !request.authorizedDocumentIds().isEmpty()
                && !request.authorizedDocumentIds().contains(request.documentId())) {
            return PolicyDecision.block("UNAUTHORIZED_DOCUMENT", "Requested document is outside the caller's allowed document set.");
        }

        if (request.roles() != null
                && !request.roles().isEmpty()
                && request.roles().stream().noneMatch(this::isKnowledgeRole)) {
            return PolicyDecision.block("MISSING_KNOWLEDGE_ROLE", "Caller role is not allowed to retrieve knowledge.");
        }

        return PolicyDecision.allow();
    }

    public PolicyDecision inspectEvidence(CitationPackage citationPackage, String sensitivityClearance) {
        if (citationPackage == null || citationPackage.citations() == null) {
            return PolicyDecision.allow();
        }

        for (Citation citation : citationPackage.citations()) {
            String excerpt = citation.excerpt() == null ? "" : citation.excerpt();
            if (containsPromptInjection(excerpt)) {
                return PolicyDecision.block("PROMPT_INJECTION_EVIDENCE", "Retrieved document content contains instruction-override language.");
            }
            if (containsSensitiveData(excerpt) && !"HIGH".equalsIgnoreCase(sensitivityClearance)) {
                return PolicyDecision.block("DLP_EVIDENCE_BLOCK", "Retrieved evidence contains sensitive data and requires high clearance.");
            }
        }

        return PolicyDecision.allow();
    }

    public RetrievalEventResponse recordRetrieval(
            KnowledgeQueryRequest request,
            String status,
            String blockedReason,
            int citationCount,
            Double confidence,
            long latencyMs
    ) {
        RetrievalEvent event = new RetrievalEvent(
                tenantId(request),
                request == null ? "default-workspace" : defaultIfBlank(request.workspaceId(), "default-workspace"),
                request == null ? "anonymous" : defaultIfBlank(request.userId(), "anonymous"),
                request == null ? null : request.documentId(),
                request == null ? "" : request.query(),
                status,
                blockedReason,
                citationCount,
                confidence,
                latencyMs
        );
        retrievalEvents.put(event.id(), event);
        return toRetrievalEventResponse(event);
    }

    public KnowledgeFeedbackResponse recordFeedback(KnowledgeFeedbackRequest request) {
        FeedbackEvent event = new FeedbackEvent(
                request.retrievalEventId(),
                request.documentId(),
                RegulatedRequestContext.tenantId(),
                request.userId(),
                request.category(),
                defaultIfBlank(request.severity(), "MEDIUM"),
                request.message()
        );
        feedbackEvents.put(event.id(), event);
        return toFeedbackResponse(event);
    }

    public RetrievalEventsResponse listRetrievalEvents(String tenantId, String status, int limit) {
        List<RetrievalEventResponse> events = retrievalEvents.values().stream()
                .filter(event -> !hasText(tenantId) || tenantId.equals(event.tenantId()))
                .filter(event -> !hasText(status) || status.equalsIgnoreCase(event.status()))
                .sorted(Comparator.comparing(RetrievalEvent::createdAt).reversed())
                .limit(safeLimit(limit))
                .map(this::toRetrievalEventResponse)
                .toList();
        return new RetrievalEventsResponse(events, events.size());
    }

    public KnowledgeEvaluationResponse runEvaluation() {
        List<RetrievalEvent> events = new ArrayList<>(retrievalEvents.values());
        int total = events.size();
        int grounded = (int) events.stream().filter(event -> "ANSWERED".equals(event.status())).count();
        int blocked = (int) events.stream().filter(event -> "BLOCKED".equals(event.status())).count();
        int noEvidence = (int) events.stream().filter(event -> "NO_EVIDENCE".equals(event.status())).count();
        double avgConfidence = events.stream()
                .filter(event -> event.confidence() != null)
                .mapToDouble(RetrievalEvent::confidence)
                .average()
                .orElse(0.0);

        return new KnowledgeEvaluationResponse(
                total,
                grounded,
                blocked,
                noEvidence,
                total == 0 ? 0.0 : grounded / (double) total,
                total == 0 ? 0.0 : blocked / (double) total,
                avgConfidence
        );
    }

    public KnowledgeOperationsMetricsResponse metrics() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        retrievalEvents.values().forEach(event ->
                byStatus.merge(event.status(), 1L, Long::sum));

        return new KnowledgeOperationsMetricsResponse(
                retrievalEvents.size(),
                feedbackEvents.size(),
                byStatus.getOrDefault("BLOCKED", 0L),
                byStatus.getOrDefault("NO_EVIDENCE", 0L),
                byStatus
        );
    }

    private boolean containsPromptInjection(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.contains("ignore previous instructions")
                || lower.contains("ignore all previous instructions")
                || lower.contains("reveal system prompt")
                || lower.contains("developer message")
                || lower.contains("disable safety")
                || lower.contains("jailbreak")
                || lower.contains("exfiltrate");
    }

    private boolean containsSensitiveData(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return SSN_PATTERN.matcher(value == null ? "" : value).find()
                || CARD_PATTERN.matcher(value == null ? "" : value).find()
                || lower.contains("api key")
                || lower.contains("secret key")
                || lower.contains("password=")
                || lower.contains("private key");
    }

    private boolean isKnowledgeRole(String role) {
        String lower = role == null ? "" : role.toLowerCase(Locale.ROOT);
        return lower.equals("knowledge.read")
                || lower.equals("knowledge.admin")
                || lower.equals("knowledge-curator")
                || lower.equals("curator")
                || lower.equals("admin");
    }

    private String tenantId(KnowledgeQueryRequest request) {
        if (request != null && hasText(request.tenantId())) {
            return request.tenantId();
        }
        return RegulatedRequestContext.tenantId();
    }

    private RetrievalEventResponse toRetrievalEventResponse(RetrievalEvent event) {
        return new RetrievalEventResponse(
                event.id(),
                event.tenantId(),
                event.workspaceId(),
                event.userId(),
                event.documentId(),
                event.query(),
                event.status(),
                event.blockedReason(),
                event.citationCount(),
                event.confidence(),
                event.latencyMs(),
                event.createdAt()
        );
    }

    private KnowledgeFeedbackResponse toFeedbackResponse(FeedbackEvent event) {
        return new KnowledgeFeedbackResponse(
                event.id(),
                event.retrievalEventId(),
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

    public record PolicyDecision(boolean allowed, String reasonCode, String message) {
        static PolicyDecision allow() {
            return new PolicyDecision(true, null, null);
        }

        static PolicyDecision block(String reasonCode, String message) {
            return new PolicyDecision(false, reasonCode, message);
        }
    }

    private record RetrievalEvent(
            UUID id,
            String tenantId,
            String workspaceId,
            String userId,
            String documentId,
            String query,
            String status,
            String blockedReason,
            int citationCount,
            Double confidence,
            long latencyMs,
            Instant createdAt
    ) {
        RetrievalEvent(
                String tenantId,
                String workspaceId,
                String userId,
                String documentId,
                String query,
                String status,
                String blockedReason,
                int citationCount,
                Double confidence,
                long latencyMs
        ) {
            this(UUID.randomUUID(), tenantId, workspaceId, userId, documentId, query, status, blockedReason, citationCount, confidence, latencyMs, Instant.now());
        }
    }

    private record FeedbackEvent(
            UUID id,
            String retrievalEventId,
            String documentId,
            String tenantId,
            String userId,
            String category,
            String severity,
            String message,
            String status,
            Instant createdAt
    ) {
        FeedbackEvent(
                String retrievalEventId,
                String documentId,
                String tenantId,
                String userId,
                String category,
                String severity,
                String message
        ) {
            this(UUID.randomUUID(), retrievalEventId, documentId, tenantId, userId, category, severity, message, "OPEN", Instant.now());
        }
    }
}
