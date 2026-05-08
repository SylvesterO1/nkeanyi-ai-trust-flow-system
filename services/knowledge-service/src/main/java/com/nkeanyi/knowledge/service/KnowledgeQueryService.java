package com.nkeanyi.knowledge.service;

import com.nkeanyi.knowledge.citation.CitationPackage;
import com.nkeanyi.knowledge.citation.CitationPackager;
import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import com.nkeanyi.knowledge.embedding.EmbeddingProvider;
import com.nkeanyi.knowledge.model.KnowledgeIndexResponse;
import com.nkeanyi.knowledge.model.KnowledgeQueryResponse;
import com.nkeanyi.knowledge.model.PackagedKnowledgeResponse;
import com.nkeanyi.knowledge.qdrant.QdrantRestClient;
import com.nkeanyi.knowledge.retrieval.MultiQueryRetrievalService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeQueryService {

    private final EmbeddingProvider embeddingProvider;
    private final QdrantRestClient qdrantRestClient;
    private final KnowledgeServiceProperties properties;
    private final CitationPackager citationPackager;
    private final MultiQueryRetrievalService multiQueryRetrievalService;

    public KnowledgeQueryService(
            EmbeddingProvider embeddingProvider,
            QdrantRestClient qdrantRestClient,
            KnowledgeServiceProperties properties,
            CitationPackager citationPackager,
            MultiQueryRetrievalService multiQueryRetrievalService
    ) {
        this.embeddingProvider = embeddingProvider;
        this.qdrantRestClient = qdrantRestClient;
        this.properties = properties;
        this.citationPackager = citationPackager;
        this.multiQueryRetrievalService = multiQueryRetrievalService;
    }

    public KnowledgeIndexResponse index(String source, String content) {
        return index(UUID.randomUUID().toString(), content, source, 1, 0, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", false, 0.0);
    }

    public KnowledgeIndexResponse index(String pointId, String content, String source, int pageNumber, int chunkIndex) {
        return index(pointId, content, source, pageNumber, chunkIndex, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", false, 0.0);
    }

    public KnowledgeIndexResponse index(
            String pointId,
            String content,
            String source,
            int pageNumber,
            int chunkIndex,
            String documentType,
            String extractorUsed,
            boolean ocrUsed,
            double confidence
    ) {
        String extractionMode = ocrUsed ? "OCR" : "PARSED";
        return index(pointId, content, source, pageNumber, chunkIndex, documentType, "UNKNOWN", extractorUsed, extractionMode, ocrUsed, confidence);
    }

    public KnowledgeIndexResponse index(
            String pointId,
            String content,
            String source,
            int pageNumber,
            int chunkIndex,
            String documentType,
            String sectionTitle,
            String extractorUsed,
            String extractionMode,
            boolean ocrUsed,
            double confidence
    ) {
        float[] vector = embeddingProvider.embed(content);

        qdrantRestClient.upsertPoint(
                pointId,
                vector,
                Map.ofEntries(
                        Map.entry("source", safeSource(source)),
                        Map.entry("content", safe(content)),
                        Map.entry("pageNumber", pageNumber),
                        Map.entry("chunkIndex", chunkIndex),
                        Map.entry("documentType", safe(documentType)),
                        Map.entry("sectionTitle", safe(sectionTitle)),
                        Map.entry("extractorUsed", safe(extractorUsed)),
                        Map.entry("extractionMode", safe(extractionMode)),
                        Map.entry("ocrUsed", ocrUsed),
                        Map.entry("confidence", confidence),
                        Map.entry("indexedAt", Instant.now().toString())
                )
        );

        return new KnowledgeIndexResponse(
                pointId,
                properties.qdrant().collection(),
                "INDEXED"
        );
    }

    public KnowledgeQueryResponse search(String query) {
        return new KnowledgeQueryResponse(
                query,
                multiQueryRetrievalService.retrieve(
                        query,
                        properties.retrieval().candidateLimit()
                )
        );
    }

    public PackagedKnowledgeResponse searchWithCitations(String query) {
        KnowledgeQueryResponse retrieval = search(query);
        CitationPackage citationPackage = citationPackager.pack(query, retrieval.chunks());
        return new PackagedKnowledgeResponse(retrieval, citationPackage);
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "UNKNOWN" : value;
    }

    private String safeSource(String source) {
        return (source == null || source.isBlank()) ? "unknown-source" : source;
    }
}
