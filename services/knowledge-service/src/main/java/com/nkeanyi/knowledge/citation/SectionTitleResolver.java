package com.nkeanyi.knowledge.citation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SectionTitleResolver {

    private static final Pattern NUMBERED_HEADING =
            Pattern.compile("^\\s*(\\d+(\\.\\d+)*)\\s+.+$");

    public String resolve(String content, String documentType) {
        if (content == null || content.isBlank()) {
            return fallback(documentType);
        }

        List<String> lines = content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .limit(6)
                .toList();

        for (String line : lines) {
            if (looksLikeHeading(line)) {
                return normalizeHeading(line);
            }
        }

        return inferFromSentence(content, documentType);
    }

    private boolean looksLikeHeading(String line) {
        if (line.length() > 80) {
            return false;
        }

        if (NUMBERED_HEADING.matcher(line).matches()) {
            return true;
        }

        if (isMostlyUppercase(line) && line.length() >= 4) {
            return true;
        }

        return isTitleLike(line);
    }

    private boolean isMostlyUppercase(String line) {
        long letters = line.chars().filter(Character::isLetter).count();
        if (letters == 0) {
            return false;
        }

        long uppercase = line.chars().filter(Character::isUpperCase).count();
        return ((double) uppercase / letters) >= 0.70;
    }

    private boolean isTitleLike(String line) {
        String[] words = line.split("\\s+");
        if (words.length == 0 || words.length > 8) {
            return false;
        }

        int titleWords = 0;
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            char first = word.charAt(0);
            if (Character.isUpperCase(first)) {
                titleWords++;
            }
        }

        return titleWords >= Math.max(2, words.length / 2);
    }

    private String normalizeHeading(String line) {
        String cleaned = line.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[0-9.]+\\s+", "");
        return toTitleCase(cleaned);
    }

    private String inferFromSentence(String content, String documentType) {
        String text = content.replaceAll("\\s+", " ").trim();

        if (text.toLowerCase(Locale.ROOT).contains("high-risk customers")) {
            return "High-Risk Customers";
        }
        if (text.toLowerCase(Locale.ROOT).contains("enhanced due diligence")) {
            return "Enhanced Due Diligence";
        }
        if (text.toLowerCase(Locale.ROOT).contains("compliance review")) {
            return "Compliance Review";
        }
        if (text.toLowerCase(Locale.ROOT).contains("monitoring")) {
            return "Monitoring Requirements";
        }

        return fallback(documentType);
    }

    private String fallback(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return "General";
        }
        return toTitleCase(documentType.replace('_', ' '));
    }

    private String toTitleCase(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1));
            }
        }

        return sb.toString().trim();
    }
}
