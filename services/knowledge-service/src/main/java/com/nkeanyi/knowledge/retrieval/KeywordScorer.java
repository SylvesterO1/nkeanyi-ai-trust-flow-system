package com.nkeanyi.knowledge.retrieval;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeywordScorer {

    public double score(String query, String content) {
        Set<String> queryTerms = tokenize(query);
        Set<String> contentTerms = tokenize(content);

        if (queryTerms.isEmpty() || contentTerms.isEmpty()) {
            return 0.0;
        }

        long matched = queryTerms.stream()
                .filter(contentTerms::contains)
                .count();

        return (double) matched / queryTerms.size();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
