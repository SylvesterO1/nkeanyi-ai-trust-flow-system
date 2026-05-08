package com.nkeanyi.knowledge.embedding;

public interface EmbeddingProvider {
    float[] embed(String text);
}
