package com.nkeanyi.knowledge.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QdrantRestClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KnowledgeServiceProperties properties;

    public QdrantRestClient(ObjectMapper objectMapper, KnowledgeServiceProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl("http://" + properties.qdrant().host() + ":" + properties.qdrant().port())
                .build();
    }

    public void ensureCollection() {
        String collection = properties.qdrant().collection();

        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", properties.qdrant().vectorSize(),
                        "distance", "Cosine"
                )
        );

        try {
            restClient.put()
                    .uri("/collections/{collection}", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
        }
    }

    public void upsertPoint(String id, float[] vector, Map<String, Object> payload) {
        String collection = properties.qdrant().collection();

        Map<String, Object> point = new LinkedHashMap<>();
        point.put("id", id);
        point.put("vector", toList(vector));
        point.put("payload", payload);

        Map<String, Object> body = Map.of("points", List.of(point));

        restClient.put()
                .uri("/collections/{collection}/points", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public List<SearchHit> search(float[] vector, int limit) {
        String collection = properties.qdrant().collection();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vector", toList(vector));
        body.put("limit", limit);
        body.put("with_payload", true);

        String response = restClient.post()
                .uri("/collections/{collection}/points/search", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("result");
            if (!result.isArray()) {
                return List.of();
            }

            List<SearchHit> hits = new ArrayList<>();
            for (JsonNode item : result) {
                String id = item.path("id").asText();
                double score = item.path("score").asDouble();
                JsonNode payload = item.path("payload");

                hits.add(new SearchHit(
                        id,
                        score,
                        payload.path("source").asText("unknown"),
                        payload.path("content").asText(""),
                        payload.path("pageNumber").asInt(1),
                        payload.path("documentType").asText("UNKNOWN"),
                        payload.path("sectionTitle").asText("UNKNOWN"),
                        payload.path("extractorUsed").asText("UNKNOWN"),
                        payload.path("extractionMode").asText("UNKNOWN"),
                        payload.path("ocrUsed").asBoolean(false),
                        payload.path("confidence").asDouble(0.0),
                        payload
                ));
            }
            return hits;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Qdrant search response", e);
        }
    }

    private List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float v : vector) {
            values.add(v);
        }
        return values;
    }

    public record SearchHit(
            String id,
            double score,
            String source,
            String content,
            int pageNumber,
            String documentType,
            String sectionTitle,
            String extractorUsed,
            String extractionMode,
            boolean ocrUsed,
            double confidence,
            JsonNode payload
    ) {}
}
