package com.nkeanyi.knowledge.embedding.ollama;

import java.util.List;

public record OllamaEmbeddingResponse(
        List<Double> embedding
) {}
