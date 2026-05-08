package com.nkeanyi.documentintelligence.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SectionTitleResolver {

    public List<SectionBlock> splitIntoSections(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] lines = text.split("\\R");
        List<SectionBlock> blocks = new ArrayList<>();

        String currentTitle = "UNKNOWN";
        StringBuilder currentContent = new StringBuilder();

        for (String rawLine : lines) {
            String line = normalize(rawLine);

            if (line.isBlank()) {
                continue;
            }

            if (looksLikeHeading(line)) {
                if (currentContent.length() > 0) {
                    blocks.add(new SectionBlock(currentTitle, currentContent.toString().trim()));
                    currentContent.setLength(0);
                }
                currentTitle = line;
            } else {
                currentContent.append(line).append("\n");
            }
        }

        if (currentContent.length() > 0) {
            blocks.add(new SectionBlock(currentTitle, currentContent.toString().trim()));
        }

        return blocks;
    }

    private boolean looksLikeHeading(String line) {
        if (line.length() > 60) {
            return false;
        }

        if (line.endsWith(":")) {
            return true;
        }

        return switch (line.toLowerCase()) {
            case "purpose", "core requirements", "control summary", "reference",
                 "introduction", "scope", "definitions", "requirements",
                 "monitoring", "compliance", "escalation" -> true;
            default -> isTitleCaseLike(line);
        };
    }

    private boolean isTitleCaseLike(String line) {
        String[] words = line.split("\\s+");
        if (words.length > 6 || words.length < 1) {
            return false;
        }

        int titleish = 0;
        for (String word : words) {
            if (!word.isBlank() && Character.isUpperCase(word.charAt(0))) {
                titleish++;
            }
        }

        return titleish >= Math.max(1, words.length - 1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    public record SectionBlock(
            String sectionTitle,
            String content
    ) {}
}
