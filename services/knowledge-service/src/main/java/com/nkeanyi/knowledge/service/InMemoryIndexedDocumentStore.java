package com.nkeanyi.knowledge.service;

import com.nkeanyi.knowledge.model.KnowledgeQueryResponse;
import com.nkeanyi.knowledge.retrieval.KeywordScorer;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryIndexedDocumentStore {

    private final ConcurrentMap<String, List<KnowledgeQueryResponse.ChunkResult>> chunksByDocumentId =
            new ConcurrentHashMap<>();
    private final KeywordScorer keywordScorer;

    public InMemoryIndexedDocumentStore(KeywordScorer keywordScorer) {
        this.keywordScorer = keywordScorer;
    }

    public void save(String documentId, KnowledgeQueryResponse.ChunkResult chunk) {
        String key = safeDocumentId(documentId);
        chunksByDocumentId.compute(key, (ignored, existing) -> {
            if (existing == null) {
                return List.of(chunk);
            }

            var updated = new java.util.ArrayList<>(existing);
            updated.add(chunk);
            return List.copyOf(updated);
        });
    }

    public List<KnowledgeQueryResponse.ChunkResult> search(String documentId, String query, int limit) {
        return chunksByDocumentId.getOrDefault(safeDocumentId(documentId), List.of())
                .stream()
                .map(chunk -> new KnowledgeQueryResponse.ChunkResult(
                        chunk.id(),
                        chunk.source(),
                        chunk.content(),
                        chunk.pageNumber(),
                        chunk.documentType(),
                        chunk.sectionTitle(),
                        chunk.extractorUsed(),
                        chunk.extractionMode(),
                        chunk.ocrUsed(),
                        keywordScorer.score(query, chunk.content()),
                        chunk.confidence()
                ))
                .sorted(Comparator.comparingDouble(KnowledgeQueryResponse.ChunkResult::score).reversed())
                .limit(limit)
                .toList();
    }

    private String safeDocumentId(String documentId) {
        return documentId == null || documentId.isBlank() ? "unknown-document" : documentId;
    }
}
