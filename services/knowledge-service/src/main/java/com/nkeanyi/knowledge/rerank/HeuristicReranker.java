package com.nkeanyi.knowledge.rerank;

import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import com.nkeanyi.knowledge.retrieval.HybridRanker;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class HeuristicReranker {

    private final KnowledgeServiceProperties properties;

    public HeuristicReranker(KnowledgeServiceProperties properties) {
        this.properties = properties;
    }

    public List<RerankedHit> rerank(String query, List<HybridRanker.HybridHit> hits) {
        int topK = Math.min(properties.reranking().topK(), hits.size());

        return hits.stream()
                .limit(topK)
                .map(hit -> {
                    double phraseScore = phraseScore(query, hit.searchHit().content());
                    double densityScore = densityScore(query, hit.searchHit().content());
                    double lengthScore = lengthScore(hit.searchHit().content());

                    double rerankBoost =
                            (phraseScore * properties.reranking().phraseWeight()) +
                            (densityScore * properties.reranking().densityWeight()) +
                            (lengthScore * properties.reranking().lengthWeight());

                    double finalScore = hit.fusedScore() + rerankBoost;

                    return new RerankedHit(
                            hit,
                            phraseScore,
                            densityScore,
                            lengthScore,
                            finalScore
                    );
                })
                .sorted(Comparator.comparingDouble(RerankedHit::finalScore).reversed())
                .toList();
    }

    private double phraseScore(String query, String content) {
        if (query == null || content == null) return 0.0;
        String q = query.toLowerCase(Locale.ROOT).trim();
        String c = content.toLowerCase(Locale.ROOT);

        if (c.contains(q)) {
            return 1.0;
        }

        List<String> phrases = slidingPhrases(q, 2);
        long matches = phrases.stream().filter(c::contains).count();

        return phrases.isEmpty() ? 0.0 : (double) matches / phrases.size();
    }

    private double densityScore(String query, String content) {
        Set<String> qTerms = tokenize(query);
        List<String> cTerms = new ArrayList<>(tokenize(content));
        if (qTerms.isEmpty() || cTerms.isEmpty()) return 0.0;

        long matches = cTerms.stream().filter(qTerms::contains).count();
        return (double) matches / cTerms.size();
    }

    private double lengthScore(String content) {
        int len = content == null ? 0 : content.length();
        if (len == 0) return 0.0;

        if (len >= 200 && len <= 1200) return 1.0;
        if (len < 200) return (double) len / 200.0;
        return Math.max(0.2, 1200.0 / len);
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();

        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> slidingPhrases(String text, int window) {
        List<String> tokens = Arrays.stream(text.split("\\W+"))
                .filter(t -> !t.isBlank())
                .toList();

        List<String> phrases = new ArrayList<>();
        for (int i = 0; i <= tokens.size() - window; i++) {
            phrases.add(String.join(" ", tokens.subList(i, i + window)));
        }
        return phrases;
    }

    public record RerankedHit(
            HybridRanker.HybridHit hybridHit,
            double phraseScore,
            double densityScore,
            double lengthScore,
            double finalScore
    ) {}
}
