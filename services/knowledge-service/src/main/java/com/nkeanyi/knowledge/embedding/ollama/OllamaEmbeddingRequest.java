package com.nkeanyi.knowledge.embedding.ollama;

public record OllamaEmbeddingRequest(
        String model,
        String prompt
) {}
