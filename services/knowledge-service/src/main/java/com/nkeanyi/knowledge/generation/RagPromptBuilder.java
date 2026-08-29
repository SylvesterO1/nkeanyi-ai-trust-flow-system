package com.nkeanyi.knowledge.generation;

import com.nkeanyi.knowledge.citation.CitationPackage;
import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import org.springframework.stereotype.Component;

@Component
public class RagPromptBuilder {

    private final KnowledgeServiceProperties properties;

    public RagPromptBuilder(KnowledgeServiceProperties properties) {
        this.properties = properties;
    }

    public String build(String query, CitationPackage citationPackage) {
        String context = truncate(citationPackage.assembledContext(), properties.generation().maxContextChars());

        return """
                %s

                User question:
                %s

                Evidence:
                %s

                Instructions:
                - Answer using only the evidence above.
                - If the evidence is insufficient, say that clearly.
                - Cite supporting evidence inline using [number].
                - Be concise and factual.
                """.formatted(
                properties.generation().systemPrompt(),
                query,
                context
        );
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max);
    }
}
