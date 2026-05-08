package com.nkeanyi.documentintelligence.integration;

import com.nkeanyi.documentintelligence.config.DocumentIntelligenceProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KnowledgeServiceClient {

    private final RestClient restClient;

    public KnowledgeServiceClient(DocumentIntelligenceProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.knowledge().baseUrl())
                .build();
    }

    public String indexExtractedText(String source, String content) {
        try {
            String response = restClient.post()
                    .uri("/api/v1/knowledge/index-text")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new KnowledgeIndexRequest(source, content))
                    .retrieve()
                    .body(String.class);

            return response == null ? "UNKNOWN" : response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed calling knowledge-service: " + e.getMessage(), e);
        }
    }
}
