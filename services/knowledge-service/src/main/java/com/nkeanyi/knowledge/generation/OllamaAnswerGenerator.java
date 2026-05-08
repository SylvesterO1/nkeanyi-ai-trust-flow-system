package com.nkeanyi.knowledge.generation;

import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaAnswerGenerator {

    private final RestClient restClient;
    private final KnowledgeServiceProperties properties;

    public OllamaAnswerGenerator(KnowledgeServiceProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.ollama().baseUrl())
                .build();
    }

    public String generateAnswer(String prompt) {
        OllamaChatResponse response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OllamaChatRequest(
                        properties.ollama().chatModel(),
                        prompt,
                        false
                ))
                .retrieve()
                .body(OllamaChatResponse.class);

        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new IllegalStateException("Ollama returned no answer");
        }

        return response.response().trim();
    }
}
