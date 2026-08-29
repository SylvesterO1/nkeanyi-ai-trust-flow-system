package com.nkeanyi.knowledge.retrieval;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class QueryExpander {

    public List<String> expand(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Set<String> queries = new LinkedHashSet<>();
        String normalized = query.trim();

        queries.add(normalized);
        queries.add(normalized + " requirements");
        queries.add("policy for " + normalized);
        queries.add(normalized + " compliance rules");

        return queries.stream().limit(4).toList();
    }
}
