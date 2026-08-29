package com.nkeanyi.knowledge.service;

import com.nkeanyi.knowledge.citation.Citation;
import com.nkeanyi.knowledge.citation.CitationPackage;
import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import com.nkeanyi.knowledge.generation.OllamaAnswerGenerator;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerFailedEvent;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerFailedPublisher;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerGeneratedEvent;
import com.nkeanyi.knowledge.kafka.KnowledgeAnswerGeneratedPublisher;
import com.nkeanyi.knowledge.model.KnowledgeQueryResponse;
import com.nkeanyi.knowledge.model.PackagedKnowledgeResponse;
import com.nkeanyi.knowledge.retrieval.QueryIntentGuard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAnswerServiceFailureTest {

    @Test
    void documentIdNotFoundPublishesKnowledgeAnswerFailedEvidenceEvent() {
        CapturingGeneratedPublisher generatedPublisher = new CapturingGeneratedPublisher();
        CapturingFailedPublisher failedPublisher = new CapturingFailedPublisher();
        KnowledgeAnswerService service = service(emptyQueryService(), answerGenerator("unused", false), generatedPublisher, failedPublisher);

        var response = service.answer("What does the onboarding policy require before activation?", "missing-doc", "idem-missing");

        assertThat(response.answer()).contains("could not find grounded evidence");
        assertThat(failedPublisher.events).hasSize(1);
        assertThat(failedPublisher.events.get(0).errorType()).isEqualTo("NO_GROUNDED_CHUNKS");
        assertThat(failedPublisher.events.get(0).errorCode()).isEqualTo("NO_GROUNDED_CHUNKS");
        assertThat(failedPublisher.events.get(0).recoverable()).isTrue();
        assertThat(failedPublisher.events.get(0).sourceService()).isEqualTo("knowledge-service");
        assertThat(failedPublisher.events.get(0).documentId()).isEqualTo("missing-doc");
    }

    @Test
    void noChunksFoundPublishesKnowledgeAnswerFailedEvidenceEvent() {
        CapturingFailedPublisher failedPublisher = new CapturingFailedPublisher();
        KnowledgeAnswerService service = service(emptyQueryService(), answerGenerator("unused", false), new CapturingGeneratedPublisher(), failedPublisher);

        service.answer("Which policy requires sanctions screening before activation?", null, "idem-no-chunks");

        assertThat(failedPublisher.events).hasSize(1);
        assertThat(failedPublisher.events.get(0).errorMessage()).contains("No grounded chunks found");
    }

    @Test
    void modelFailureFallsBackToGroundedCitationAndPublishesFailureEvidence() {
        CapturingGeneratedPublisher generatedPublisher = new CapturingGeneratedPublisher();
        CapturingFailedPublisher failedPublisher = new CapturingFailedPublisher();
        KnowledgeAnswerService service = service(citedQueryService(), answerGenerator(null, true), generatedPublisher, failedPublisher);

        var response = service.answer("Which policy requires sanctions screening before activation?", "doc-1", "idem-model-fail");

        assertThat(response.answer()).contains("Sanctions screening is required").contains("[1]");
        assertThat(response.citationPackage().citations()).hasSize(1);
        assertThat(failedPublisher.events).hasSize(1);
        assertThat(failedPublisher.events.get(0).errorType()).isEqualTo("IllegalStateException");
        assertThat(generatedPublisher.events).hasSize(1);
    }

    @Test
    void duplicateIdempotencyKeyDoesNotRepublishKnowledgeAnswerGeneratedEvent() {
        CapturingGeneratedPublisher generatedPublisher = new CapturingGeneratedPublisher();
        CapturingFailedPublisher failedPublisher = new CapturingFailedPublisher();
        KnowledgeAnswerService service = service(citedQueryService(), answerGenerator("Sanctions screening is required [1]", false), generatedPublisher, failedPublisher);

        var first = service.answer("Which policy requires sanctions screening before activation?", "doc-1", "idem-repeat");
        var second = service.answer("Which policy requires sanctions screening before activation?", "doc-1", "idem-repeat");

        assertThat(second).isEqualTo(first);
        assertThat(generatedPublisher.events).hasSize(1);
        assertThat(failedPublisher.events).isEmpty();
    }

    private KnowledgeAnswerService service(
            KnowledgeQueryService queryService,
            OllamaAnswerGenerator answerGenerator,
            CapturingGeneratedPublisher generatedPublisher,
            CapturingFailedPublisher failedPublisher
    ) {
        return new KnowledgeAnswerService(
                queryService,
                answerGenerator,
                new QueryIntentGuard(),
                generatedPublisher,
                failedPublisher
        );
    }

    private KnowledgeQueryService emptyQueryService() {
        return new FakeKnowledgeQueryService(new CitationPackage("query", List.of(), ""));
    }

    private KnowledgeQueryService citedQueryService() {
        Citation citation = new Citation(
                1,
                "chunk-1",
                "doc-1",
                1,
                "POLICY",
                "Control Summary",
                "Sanctions screening is required before activation.",
                0.95
        );
        return new FakeKnowledgeQueryService(new CitationPackage("query", List.of(citation), citation.excerpt()));
    }

    private OllamaAnswerGenerator answerGenerator(String answer, boolean fail) {
        return new OllamaAnswerGenerator(properties()) {
            @Override
            public String generateAnswer(String prompt) {
                if (fail) {
                    throw new IllegalStateException("ollama unavailable");
                }
                return answer;
            }
        };
    }

    private KnowledgeServiceProperties properties() {
        return new KnowledgeServiceProperties(
                new KnowledgeServiceProperties.Qdrant("localhost", 6333, "test", 8),
                new KnowledgeServiceProperties.Minio("http://localhost:9000", "access", "secret", "bucket"),
                new KnowledgeServiceProperties.Embedding("local-hash"),
                new KnowledgeServiceProperties.Chunking(1000, 100, 10),
                new KnowledgeServiceProperties.Ollama("http://localhost:11434", "embed", "chat"),
                new KnowledgeServiceProperties.Retrieval(5, 0.7, 0.3),
                new KnowledgeServiceProperties.Reranking(5, 0.4, 0.4, 0.2),
                new KnowledgeServiceProperties.Generation(4000, "system")
        );
    }

    private class FakeKnowledgeQueryService extends KnowledgeQueryService {
        private final CitationPackage citationPackage;

        FakeKnowledgeQueryService(CitationPackage citationPackage) {
            super(null, null, properties(), null, null, null);
            this.citationPackage = citationPackage;
        }

        @Override
        public PackagedKnowledgeResponse searchWithCitations(String query, String documentId) {
            return new PackagedKnowledgeResponse(new KnowledgeQueryResponse(query, List.of()), citationPackage);
        }
    }

    private static class CapturingGeneratedPublisher extends KnowledgeAnswerGeneratedPublisher {
        private final java.util.List<KnowledgeAnswerGeneratedEvent> events = new java.util.ArrayList<>();

        CapturingGeneratedPublisher() {
            super(null, "knowledge.answer.generated.v1");
        }

        @Override
        public void publish(KnowledgeAnswerGeneratedEvent event) {
            events.add(event);
        }
    }

    private static class CapturingFailedPublisher extends KnowledgeAnswerFailedPublisher {
        private final java.util.List<KnowledgeAnswerFailedEvent> events = new java.util.ArrayList<>();

        CapturingFailedPublisher() {
            super(null, "knowledge.answer.failed.v1");
        }

        @Override
        public void publish(KnowledgeAnswerFailedEvent event) {
            events.add(event);
        }
    }
}
