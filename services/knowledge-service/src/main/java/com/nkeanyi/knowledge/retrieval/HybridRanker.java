package com.nkeanyi.knowledge.retrieval;

import com.nkeanyi.knowledge.qdrant.QdrantRestClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HybridRanker {

    private static final double MIN_FUSED_SCORE = 0.45;

    public List<HybridHit> rerank(String query, List<QdrantRestClient.SearchHit> candidates) {
        if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Set<String> queryTerms = tokenize(query);
        String loweredQuery = query.toLowerCase(Locale.ROOT);

        return candidates.stream()
                .map(hit -> toHybridHit(loweredQuery, queryTerms, hit))
                .filter(hit -> hit.fusedScore() >= MIN_FUSED_SCORE)
                .sorted(Comparator.comparingDouble(HybridHit::fusedScore).reversed())
                .toList();
    }

    private HybridHit toHybridHit(String loweredQuery, Set<String> queryTerms, QdrantRestClient.SearchHit hit) {
        String content = safeLower(hit.content());

        long overlapCount = queryTerms.stream()
                .filter(content::contains)
                .count();

        double keywordScore = queryTerms.isEmpty()
                ? 0.0
                : (double) overlapCount / queryTerms.size();

        double lengthBonus = Math.min(content.length() / 1000.0, 0.15);
        double metadataBoost = metadataBoost(loweredQuery, hit);

        double fusedScore =
                (hit.score() * 0.65) +
                (keywordScore * 0.20) +
                lengthBonus +
                metadataBoost;

        return new HybridHit(
                hit,
                hit.score(),
                keywordScore,
                lengthBonus,
                metadataBoost,
                fusedScore
        );
    }

    private double metadataBoost(String loweredQuery, QdrantRestClient.SearchHit hit) {
        String documentType = safeLower(hit.documentType());
        String source = safeLower(hit.source());
        String content = safeLower(hit.content());

        boolean policyLikeQuery =
                loweredQuery.contains("policy") ||
                loweredQuery.contains("compliance") ||
                loweredQuery.contains("requirement") ||
                loweredQuery.contains("obligation") ||
                loweredQuery.contains("control") ||
                loweredQuery.contains("high-risk customer");

        double boost = 0.0;

        if (policyLikeQuery) {
            if ("policy".equals(documentType)) {
                boost += 0.20;
            } else if ("unknown".equals(documentType)) {
                boost -= 0.03;
            }
        }

        boolean jobLikeDocument =
                source.contains("job") ||
                content.contains("how to apply") ||
                content.contains("positions with") ||
                content.contains("workday") ||
                content.contains("city of dallas");

        if (policyLikeQuery && jobLikeDocument) {
            boost -= 0.35;
        }

        return boost;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() > 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record HybridHit(
            QdrantRestClient.SearchHit searchHit,
            double vectorScore,
            double keywordScore,
            double lengthBonus,
            double metadataBoost,
            double fusedScore
    ) {}
}
