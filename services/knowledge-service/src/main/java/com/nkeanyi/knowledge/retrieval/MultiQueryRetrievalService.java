package com.nkeanyi.knowledge.retrieval;

import com.nkeanyi.knowledge.embedding.EmbeddingProvider;
import com.nkeanyi.knowledge.model.KnowledgeQueryResponse;
import com.nkeanyi.knowledge.qdrant.QdrantRestClient;
import com.nkeanyi.knowledge.rerank.HeuristicReranker;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MultiQueryRetrievalService {

    private final QueryRewriter queryRewriter;
    private final EmbeddingProvider embeddingProvider;
    private final QdrantRestClient qdrantRestClient;
    private final HybridRanker hybridRanker;
    private final HeuristicReranker heuristicReranker;

    public MultiQueryRetrievalService(
            QueryRewriter queryRewriter,
            EmbeddingProvider embeddingProvider,
            QdrantRestClient qdrantRestClient,
            HybridRanker hybridRanker,
            HeuristicReranker heuristicReranker
    ) {
        this.queryRewriter = queryRewriter;
        this.embeddingProvider = embeddingProvider;
        this.qdrantRestClient = qdrantRestClient;
        this.hybridRanker = hybridRanker;
        this.heuristicReranker = heuristicReranker;
    }

    public List<KnowledgeQueryResponse.ChunkResult> retrieve(String originalQuery, int candidateLimit) {
        List<String> queries = queryRewriter.rewrite(originalQuery);
        if (queries.isEmpty()) {
            return List.of();
        }

        Map<String, QdrantRestClient.SearchHit> mergedCandidates = new LinkedHashMap<>();

        for (String rewrittenQuery : queries) {
            float[] queryVector = embeddingProvider.embed(rewrittenQuery);

            List<QdrantRestClient.SearchHit> candidates = qdrantRestClient.search(
                    queryVector,
                    candidateLimit
            );

            for (QdrantRestClient.SearchHit hit : candidates) {
                mergedCandidates.merge(
                        hit.id(),
                        hit,
                        (existing, incoming) -> incoming.score() > existing.score() ? incoming : existing
                );
            }
        }

        List<HybridRanker.HybridHit> hybridHits = hybridRanker.rerank(
                originalQuery,
                List.copyOf(mergedCandidates.values())
        );

        return heuristicReranker.rerank(originalQuery, hybridHits)
                .stream()
                .map(hit -> new KnowledgeQueryResponse.ChunkResult(
                        hit.hybridHit().searchHit().id(),
                        hit.hybridHit().searchHit().source(),
                        hit.hybridHit().searchHit().content(),
                        hit.hybridHit().searchHit().pageNumber(),
                        hit.hybridHit().searchHit().documentType(),
                        hit.hybridHit().searchHit().sectionTitle(),
                        hit.hybridHit().searchHit().extractorUsed(),
                        hit.hybridHit().searchHit().extractionMode(),
                        hit.hybridHit().searchHit().ocrUsed(),
                        hit.finalScore(),
                        hit.hybridHit().searchHit().confidence()
                ))
                .toList();
    }
}
