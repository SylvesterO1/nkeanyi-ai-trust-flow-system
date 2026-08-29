package com.nkeanyi.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nkeanyi.knowledge")
public record KnowledgeServiceProperties(
        Qdrant qdrant,
        Minio minio,
        Embedding embedding,
        Chunking chunking,
        Ollama ollama,
        Retrieval retrieval,
        Reranking reranking,
        Generation generation
) {
    public record Qdrant(
            String host,
            int port,
            String collection,
            int vectorSize
    ) {}

    public record Minio(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket
    ) {}

    public record Embedding(
            String provider
    ) {}

    public record Chunking(
            int chunkSize,
            int chunkOverlap,
            int minChunkSize
    ) {}

    public record Ollama(
            String baseUrl,
            String embeddingModel,
            String chatModel
    ) {}

    public record Retrieval(
            int candidateLimit,
            double vectorWeight,
            double keywordWeight
    ) {}

    public record Reranking(
            int topK,
            double phraseWeight,
            double densityWeight,
            double lengthWeight
    ) {}

    public record Generation(
            int maxContextChars,
            String systemPrompt
    ) {}
}
