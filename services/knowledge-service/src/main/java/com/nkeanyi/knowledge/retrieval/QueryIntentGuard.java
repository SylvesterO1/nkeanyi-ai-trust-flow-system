package com.nkeanyi.knowledge.retrieval;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class QueryIntentGuard {

    private static final List<String> TOO_BROAD_PATTERNS = List.of(
            "how to apply",
            "how do i apply",
            "what is required",
            "what is needed",
            "where is it",
            "tell me about it"
    );

    public boolean isAmbiguous(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalized = normalize(query);

        if (normalized.split("\\s+").length <= 3) {
            return true;
        }

        return TOO_BROAD_PATTERNS.contains(normalized);
    }

    public String clarificationMessage(String query) {
        return "The question is too broad to answer reliably. Please include the subject, for example: "
                + "\"How to apply for a City of Dallas job?\" or "
                + "\"How do high-risk customer requirements apply?\"";
    }

    private String normalize(String value) {
        return Arrays.stream(value.toLowerCase(Locale.ROOT).trim().split("\\s+"))
                .filter(token -> !token.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }
}
