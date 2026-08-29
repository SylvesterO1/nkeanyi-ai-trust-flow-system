package com.nkeanyi.knowledge.embedding;

import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import org.springframework.stereotype.Component;

@Component
public class RoutingEmbeddingProvider implements EmbeddingProvider {

    private final KnowledgeServiceProperties properties;
    private final LocalHashEmbeddingProvider localHashEmbeddingProvider;
    private final OllamaEmbeddingProvider ollamaEmbeddingProvider;

    public RoutingEmbeddingProvider(
            KnowledgeServiceProperties properties,
            LocalHashEmbeddingProvider localHashEmbeddingProvider,
            OllamaEmbeddingProvider ollamaEmbeddingProvider
    ) {
        this.properties = properties;
        this.localHashEmbeddingProvider = localHashEmbeddingProvider;
        this.ollamaEmbeddingProvider = ollamaEmbeddingProvider;
    }

    @Override
    public float[] embed(String text) {
        String provider = properties.embedding().provider();

        return switch (provider) {
            case "ollama" -> ollamaEmbeddingProvider.embed(text);
            case "local-hash" -> localHashEmbeddingProvider.embed(text);
            default -> throw new IllegalArgumentException("Unsupported embedding provider: " + provider);
        };
    }
}
