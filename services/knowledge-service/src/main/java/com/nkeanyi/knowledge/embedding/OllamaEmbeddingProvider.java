package com.nkeanyi.knowledge.embedding;

import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import com.nkeanyi.knowledge.embedding.ollama.OllamaEmbeddingRequest;
import com.nkeanyi.knowledge.embedding.ollama.OllamaEmbeddingResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaEmbeddingProvider {

    private final RestClient restClient;
    private final KnowledgeServiceProperties properties;

    public OllamaEmbeddingProvider(KnowledgeServiceProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.ollama().baseUrl())
                .build();
    }

    public float[] embed(String text) {
        OllamaEmbeddingResponse response = restClient.post()
                .uri("/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OllamaEmbeddingRequest(
                        properties.ollama().embeddingModel(),
                        text
                ))
                .retrieve()
                .body(OllamaEmbeddingResponse.class);

        if (response == null || response.embedding() == null || response.embedding().isEmpty()) {
            throw new IllegalStateException("Ollama returned no embedding");
        }

        return toFloatArray(response.embedding());
    }

    private float[] toFloatArray(List<Double> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }
        return vector;
    }
}
