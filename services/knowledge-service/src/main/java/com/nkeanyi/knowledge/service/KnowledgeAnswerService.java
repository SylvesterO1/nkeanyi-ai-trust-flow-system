package com.nkeanyi.knowledge.service;

import com.nkeanyi.knowledge.citation.CitationPackage;
import com.nkeanyi.knowledge.citation.Citation;
import com.nkeanyi.knowledge.generation.OllamaAnswerGenerator;
import com.nkeanyi.knowledge.model.KnowledgeAnswerResponse;
import com.nkeanyi.knowledge.model.PackagedKnowledgeResponse;
import com.nkeanyi.knowledge.retrieval.QueryIntentGuard;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeAnswerService {

    private static final int MAX_CONTEXT_CITATIONS = 3;

    private final KnowledgeQueryService knowledgeQueryService;
    private final OllamaAnswerGenerator ollamaAnswerGenerator;
    private final QueryIntentGuard queryIntentGuard;

    public KnowledgeAnswerService(
            KnowledgeQueryService knowledgeQueryService,
            OllamaAnswerGenerator ollamaAnswerGenerator,
            QueryIntentGuard queryIntentGuard
    ) {
        this.knowledgeQueryService = knowledgeQueryService;
        this.ollamaAnswerGenerator = ollamaAnswerGenerator;
        this.queryIntentGuard = queryIntentGuard;
    }

    public KnowledgeAnswerResponse answer(String query) {
        System.out.println("KnowledgeAnswerService guard check active for query: " + query);

        if (queryIntentGuard.isAmbiguous(query)) {
            return new KnowledgeAnswerResponse(
                    query,
                    queryIntentGuard.clarificationMessage(query),
                    new CitationPackage(query, List.of(), "")
            );
        }

        PackagedKnowledgeResponse packaged = knowledgeQueryService.searchWithCitations(query);

        String answer;
        if (packaged.citationPackage() == null || packaged.citationPackage().citations().isEmpty()) {
            answer = "I could not find grounded evidence for that question in the indexed knowledge base.";
        } else {
            String compressedContext = compressContext(packaged.citationPackage().citations());
            String prompt = buildPrompt(query, compressedContext);
            answer = ollamaAnswerGenerator.generateAnswer(prompt);
        }

        return new KnowledgeAnswerResponse(
                query,
                answer,
                packaged.citationPackage()
        );
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
