package com.nkeanyi.knowledge.retrieval;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class QueryRewriter {

    private static final int MAX_QUERIES = 5;

    public List<String> rewrite(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalized = normalize(query);
        Set<String> variants = new LinkedHashSet<>();
        variants.add(normalized);

        String lowered = normalized.toLowerCase(Locale.ROOT);

        if (lowered.startsWith("what is required for ")) {
            String subject = normalized.substring("What is required for ".length()).trim();
            addIfPresent(variants, "requirements for " + subject);
            addIfPresent(variants, subject + " requirements");
            addIfPresent(variants, "obligations for " + subject);
            addIfPresent(variants, "compliance requirements for " + subject);
        }

        if (lowered.startsWith("how to apply requirements for ")) {
            String subject = normalized.substring("How to apply requirements for ".length()).trim();
            addIfPresent(variants, "requirements for " + subject);
            addIfPresent(variants, "implementation of requirements for " + subject);
            addIfPresent(variants, "controls for " + subject);
            addIfPresent(variants, "compliance obligations for " + subject);
            addIfPresent(variants, "policy for " + subject);
        }

        if (lowered.startsWith("how do you apply requirements for ")) {
            String subject = normalized.substring("How do you apply requirements for ".length()).trim();
            addIfPresent(variants, "requirements for " + subject);
            addIfPresent(variants, "implementation of requirements for " + subject);
            addIfPresent(variants, "controls for " + subject);
            addIfPresent(variants, "compliance obligations for " + subject);
        }

        if (lowered.contains("high-risk customers")) {
            addIfPresent(variants, "enhanced due diligence for high-risk customers");
            addIfPresent(variants, "monitoring and compliance review for high-risk customers");
            addIfPresent(variants, "high-risk customer compliance policy");
            addIfPresent(variants, "controls for high-risk customers");
        }

        if (lowered.startsWith("where is ")) {
            String subject = normalized.substring("Where is ".length()).trim();
            addIfPresent(variants, subject + " location");
            addIfPresent(variants, "address of " + subject);
            addIfPresent(variants, "located in " + subject);
        }

        if (lowered.startsWith("who is ")) {
            String subject = normalized.substring("Who is ".length()).trim();
            addIfPresent(variants, subject + " profile");
            addIfPresent(variants, subject + " details");
        }

        if (variants.size() < 3) {
            addKeywordBackoffQueries(variants, normalized);
        }

        return variants.stream()
                .limit(MAX_QUERIES)
                .toList();
    }

    private void addKeywordBackoffQueries(Set<String> variants, String query) {
        List<String> keywords = extractKeywords(query);

        if (keywords.size() >= 2) {
            addIfPresent(variants, String.join(" ", keywords.subList(0, Math.min(3, keywords.size()))));
        }

        if (keywords.size() >= 3) {
            addIfPresent(variants, keywords.get(0) + " " + keywords.get(1));
            addIfPresent(variants, keywords.get(1) + " " + keywords.get(2));
        }
    }

    private List<String> extractKeywords(String query) {
        String[] tokens = normalize(query).toLowerCase(Locale.ROOT).split("\\W+");
        List<String> keywords = new ArrayList<>();

        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (token.length() <= 2) {
                continue;
            }
            if (isStopWord(token)) {
                continue;
            }
            keywords.add(token);
        }

        return keywords;
    }

    private boolean isStopWord(String token) {
        return switch (token) {
            case "what", "where", "when", "which", "who", "whom", "whose",
                 "is", "are", "was", "were", "be", "been", "being",
                 "for", "the", "and", "or", "to", "of", "in", "on", "at",
                 "a", "an", "by", "with", "about", "from",
                 "how", "apply" -> true;
            default -> false;
        };
    }

    private void addIfPresent(Set<String> variants, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            variants.add(normalized);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
