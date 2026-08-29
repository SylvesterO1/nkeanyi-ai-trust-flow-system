package com.nkeanyi.knowledge.service;

import com.nkeanyi.knowledge.citation.CitationPackage;
import com.nkeanyi.knowledge.citation.Citation;
import com.nkeanyi.knowledge.generation.OllamaAnswerGenerator;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerGeneratedEvent;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerGeneratedPublisher;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerFailedEvent;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerFailedPublisher;
import com.nkeanyi.knowledge.model.KnowledgeAnswerResponse;
import com.nkeanyi.knowledge.model.KnowledgeQueryRequest;
import com.nkeanyi.knowledge.model.PackagedKnowledgeResponse;
import com.nkeanyi.knowledge.observability.RegulatedRequestContext;
import com.nkeanyi.knowledge.retrieval.QueryIntentGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class KnowledgeAnswerService {

    private static final int MAX_CONTEXT_CITATIONS = 3;

    private final KnowledgeQueryService knowledgeQueryService;
    private final OllamaAnswerGenerator ollamaAnswerGenerator;
    private final QueryIntentGuard queryIntentGuard;
    private final KnowledgeAnswerGeneratedPublisher knowledgeAnswerGeneratedPublisher;
    private final KnowledgeAnswerFailedPublisher knowledgeAnswerFailedPublisher;
    private final KnowledgeGovernanceService knowledgeGovernanceService;
    private final ConcurrentMap<String, KnowledgeAnswerResponse> answersByIdempotencyKey = new ConcurrentHashMap<>();

    @Autowired
    public KnowledgeAnswerService(
            KnowledgeQueryService knowledgeQueryService,
            OllamaAnswerGenerator ollamaAnswerGenerator,
            QueryIntentGuard queryIntentGuard,
            KnowledgeAnswerGeneratedPublisher knowledgeAnswerGeneratedPublisher,
            KnowledgeAnswerFailedPublisher knowledgeAnswerFailedPublisher,
            KnowledgeGovernanceService knowledgeGovernanceService
    ) {
        this.knowledgeQueryService = knowledgeQueryService;
        this.ollamaAnswerGenerator = ollamaAnswerGenerator;
        this.queryIntentGuard = queryIntentGuard;
        this.knowledgeAnswerGeneratedPublisher = knowledgeAnswerGeneratedPublisher;
        this.knowledgeAnswerFailedPublisher = knowledgeAnswerFailedPublisher;
        this.knowledgeGovernanceService = knowledgeGovernanceService;
    }

    public KnowledgeAnswerService(
            KnowledgeQueryService knowledgeQueryService,
            OllamaAnswerGenerator ollamaAnswerGenerator,
            QueryIntentGuard queryIntentGuard,
            KnowledgeAnswerGeneratedPublisher knowledgeAnswerGeneratedPublisher,
            KnowledgeAnswerFailedPublisher knowledgeAnswerFailedPublisher
    ) {
        this(
                knowledgeQueryService,
                ollamaAnswerGenerator,
                queryIntentGuard,
                knowledgeAnswerGeneratedPublisher,
                knowledgeAnswerFailedPublisher,
                new KnowledgeGovernanceService()
        );
    }

    public KnowledgeAnswerResponse answer(String query) {
        return answer(query, null);
    }

    public KnowledgeAnswerResponse answer(String query, String documentId) {
        return answer(query, documentId, RegulatedRequestContext.idempotencyKey());
    }

    public KnowledgeAnswerResponse answer(String query, String documentId, String idempotencyKey) {
        return answer(new KnowledgeQueryRequest(query, documentId, null, null, null, null, null, null), idempotencyKey);
    }

    public KnowledgeAnswerResponse answer(KnowledgeQueryRequest request, String idempotencyKey) {
        String safeIdempotencyKey = safeIdempotencyKey(idempotencyKey);
        KnowledgeAnswerResponse cached = answersByIdempotencyKey.get(safeIdempotencyKey);
        if (cached != null) {
            return cached;
        }
        long started = System.currentTimeMillis();
        String query = request.query();
        String documentId = request.documentId();

        KnowledgeGovernanceService.PolicyDecision authorization = knowledgeGovernanceService.authorize(request);
        if (!authorization.allowed()) {
            KnowledgeAnswerResponse response = blockedResponse(query, authorization.reasonCode(), authorization.message(), request, started);
            answersByIdempotencyKey.putIfAbsent(safeIdempotencyKey, response);
            publishFailedAnswer(documentId, safeIdempotencyKey, query, authorization.reasonCode(), authorization.message());
            return response;
        }

        if (queryIntentGuard.isAmbiguous(query)) {
            String eventId = knowledgeGovernanceService.recordRetrieval(
                    request,
                    "NEEDS_CLARIFICATION",
                    null,
                    0,
                    null,
                    elapsed(started)
            ).id().toString();
            KnowledgeAnswerResponse response = new KnowledgeAnswerResponse(
                    query,
                    queryIntentGuard.clarificationMessage(query),
                    new CitationPackage(query, List.of(), ""),
                    "NEEDS_CLARIFICATION",
                    null,
                    null,
                    eventId
            );
            if (answersByIdempotencyKey.putIfAbsent(safeIdempotencyKey, response) == null) {
                publishGeneratedAnswer(documentId, safeIdempotencyKey, response);
            }
            return response;
        }

        PackagedKnowledgeResponse packaged = knowledgeQueryService.searchWithCitations(query, documentId);

        String answer;
        String status = "ANSWERED";
        String blockedReason = null;
        Double confidence = averageConfidence(packaged.citationPackage());
        if (packaged.citationPackage() == null || packaged.citationPackage().citations().isEmpty()) {
            answer = "I could not find grounded evidence for that question in the indexed knowledge base.";
            status = "NO_EVIDENCE";
            publishFailedAnswer(
                    documentId,
                    safeIdempotencyKey,
                    query,
                    "NO_GROUNDED_CHUNKS",
                    documentId == null || documentId.isBlank()
                            ? "No grounded chunks found for query"
                            : "No grounded chunks found for documentId=" + documentId
            );
        } else {
            KnowledgeGovernanceService.PolicyDecision evidencePolicy = knowledgeGovernanceService.inspectEvidence(
                    packaged.citationPackage(),
                    request.sensitivityClearance()
            );
            if (!evidencePolicy.allowed()) {
                KnowledgeAnswerResponse response = blockedResponse(query, evidencePolicy.reasonCode(), evidencePolicy.message(), request, started);
                answersByIdempotencyKey.putIfAbsent(safeIdempotencyKey, response);
                publishFailedAnswer(documentId, safeIdempotencyKey, query, evidencePolicy.reasonCode(), evidencePolicy.message());
                return response;
            }

            String compressedContext = compressContext(packaged.citationPackage().citations());
            String prompt = buildPrompt(query, compressedContext);
            try {
                answer = ollamaAnswerGenerator.generateAnswer(prompt);
            } catch (Exception ex) {
                publishFailedAnswer(documentId, safeIdempotencyKey, query, ex.getClass().getSimpleName(), ex.getMessage());
                answer = fallbackGroundedAnswer(packaged.citationPackage().citations());
                status = "FALLBACK_ANSWER";
            }
        }

        String eventId = knowledgeGovernanceService.recordRetrieval(
                request,
                status,
                blockedReason,
                packaged.citationPackage() == null ? 0 : packaged.citationPackage().citations().size(),
                confidence,
                elapsed(started)
        ).id().toString();
        KnowledgeAnswerResponse response = new KnowledgeAnswerResponse(
                query,
                answer,
                packaged.citationPackage(),
                status,
                blockedReason,
                confidence,
                eventId
        );
        KnowledgeAnswerResponse previous = answersByIdempotencyKey.putIfAbsent(safeIdempotencyKey, response);
        if (previous != null) {
            return previous;
        }

        publishGeneratedAnswer(documentId, safeIdempotencyKey, response);
        return response;
    }

    private void publishGeneratedAnswer(String documentId, String idempotencyKey, KnowledgeAnswerResponse response) {
        knowledgeAnswerGeneratedPublisher.publish(
                new KnowledgeAnswerGeneratedEvent(
                        RegulatedRequestContext.tenantId(),
                        RegulatedRequestContext.correlationId(),
                        idempotencyKey,
                        documentId,
                        response.query(),
                        response.answer(),
                        response.citationPackage(),
                        Instant.now()
                )
        );
    }

    private KnowledgeAnswerResponse blockedResponse(
            String query,
            String reasonCode,
            String message,
            KnowledgeQueryRequest request,
            long started
    ) {
        String eventId = knowledgeGovernanceService.recordRetrieval(
                request,
                "BLOCKED",
                reasonCode,
                0,
                null,
                elapsed(started)
        ).id().toString();
        return new KnowledgeAnswerResponse(
                query,
                message,
                new CitationPackage(query, List.of(), ""),
                "BLOCKED",
                reasonCode,
                null,
                eventId
        );
    }

    private Double averageConfidence(CitationPackage citationPackage) {
        if (citationPackage == null || citationPackage.citations() == null || citationPackage.citations().isEmpty()) {
            return null;
        }
        return citationPackage.citations().stream()
                .mapToDouble(Citation::score)
                .average()
                .orElse(0.0);
    }

    private long elapsed(long started) {
        return Math.max(0, System.currentTimeMillis() - started);
    }

    private void publishFailedAnswer(
            String documentId,
            String idempotencyKey,
            String query,
            String errorType,
            String errorMessage
    ) {
        try {
            knowledgeAnswerFailedPublisher.publish(new KnowledgeAnswerFailedEvent(
                    RegulatedRequestContext.tenantId(),
                    RegulatedRequestContext.correlationId(),
                    idempotencyKey,
                    documentId,
                    query,
                    errorType,
                    errorType,
                    errorMessage,
                    true,
                    "knowledge-service",
                    Instant.now()
            ));
        } catch (Exception ignored) {
        }
    }

    private String safeIdempotencyKey(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? RegulatedRequestContext.idempotencyKey()
                : idempotencyKey;
    }

    private String compressContext(List<Citation> citations) {
        StringBuilder sb = new StringBuilder();

        citations.stream()
                .limit(MAX_CONTEXT_CITATIONS)
                .forEach(citation -> sb.append("[")
                        .append(citation.index())
                        .append("] Source: ")
                        .append(citation.source())
                        .append(" (page ")
                        .append(citation.pageNumber())
                        .append(")")
                        .append("\n")
                        .append(citation.excerpt())
                        .append("\n\n"));

        return sb.toString().trim();
    }

    private String fallbackGroundedAnswer(List<Citation> citations) {
        return citations.stream()
                .limit(MAX_CONTEXT_CITATIONS)
                .map(citation -> citation.excerpt() + " [" + citation.index() + "]")
                .findFirst()
                .orElse("I could not find grounded evidence for that question in the indexed knowledge base.");
    }

    private String buildPrompt(String query, String context) {
        return """
                You are answering a question using only the provided evidence.

                Rules:
                1. Use only the evidence in the context.
                2. Do not add outside knowledge.
                3. Answer directly and concisely.
                4. Do not say the context is partial unless the evidence truly does not answer the question.
                5. Cite claims using the citation numbers already provided, like [1] or [2].
                6. Prefer a single short paragraph unless a list is clearly necessary.
                7. When useful, preserve page-aware grounding from the evidence.

                Question:
                %s

                Evidence:
                %s

                Answer:
                """.formatted(query, context == null ? "" : context);
    }
}
