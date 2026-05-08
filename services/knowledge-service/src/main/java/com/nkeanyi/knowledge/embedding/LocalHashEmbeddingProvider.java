package com.nkeanyi.knowledge.embedding;

import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocalHashEmbeddingProvider {

    private final int vectorSize;

    public LocalHashEmbeddingProvider(KnowledgeServiceProperties properties) {
        this.vectorSize = properties.qdrant().vectorSize();
    }

    public float[] embed(String text) {
        float[] vector = new float[vectorSize];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String normalized = text.toLowerCase(Locale.ROOT).trim();
        String[] tokens = normalized.split("\\s+");

        for (String token : tokens) {
            int hash = Math.abs(token.hashCode());
            int idx = hash % vectorSize;
            vector[idx] += 1.0f;
        }

        normalize(vector);
        return vector;
    }

    private void normalize(float[] vector) {
        double sumSquares = 0.0;
        for (float v : vector) {
            sumSquares += v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
