package com.nkeanyi.knowledge.citation;

import com.nkeanyi.knowledge.model.KnowledgeQueryResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CitationPackager {

    private static final int MAX_CITATIONS = 4;

    public CitationPackage pack(String query, List<KnowledgeQueryResponse.ChunkResult> chunks) {
        List<KnowledgeQueryResponse.ChunkResult> deduped = deduplicate(chunks).stream()
                .limit(MAX_CITATIONS)
                .toList();

        List<Citation> citations = new ArrayList<>();
        StringBuilder assembledContext = new StringBuilder();

        int index = 1;
        for (KnowledgeQueryResponse.ChunkResult chunk : deduped) {
            String excerpt = trimExcerpt(chunk.content());

            citations.add(new Citation(
                    index,
                    chunk.id(),
                    chunk.source(),
                    chunk.pageNumber(),
                    chunk.documentType(),
                    chunk.sectionTitle(),
                    excerpt,
                    chunk.score()
            ));

            assembledContext.append("[")
                    .append(index)
                    .append("] Source: ")
                    .append(chunk.source())
                    .append(" (page ")
                    .append(chunk.pageNumber())
                    .append(")")
                    .append("\n")
                    .append(excerpt)
                    .append("\n\n");

            index++;
        }

        return new CitationPackage(query, citations, assembledContext.toString().trim());
    }

    private List<KnowledgeQueryResponse.ChunkResult> deduplicate(List<KnowledgeQueryResponse.ChunkResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Set<String> seen = new HashSet<>();
        List<KnowledgeQueryResponse.ChunkResult> deduped = new ArrayList<>();

        for (KnowledgeQueryResponse.ChunkResult chunk : chunks) {
            String key = normalizeForDedup(chunk.content()) + "|p:" + chunk.pageNumber();
            if (key.isBlank()) {
                continue;
            }

            if (seen.add(key)) {
                deduped.add(chunk);
            }
        }

        return deduped;
    }

    private String normalizeForDedup(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String trimExcerpt(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        int maxLength = 300;

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, maxLength) + "...";
    }
}
