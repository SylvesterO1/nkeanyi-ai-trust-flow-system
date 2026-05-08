package com.nkeanyi.documentintelligence.layout;

import com.nkeanyi.documentintelligence.extract.ExtractedPage;
import com.nkeanyi.documentintelligence.extract.ExtractionResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class LayoutAnalysisService {

    public LayoutAnalysisResult analyze(String fileName, ExtractionResult extractionResult) {
        List<LayoutPage> pages = extractionResult.pages().stream()
                .map(this::analyzePage)
                .toList();

        return new LayoutAnalysisResult(
                fileName,
                extractionResult.extractionMode(),
                extractionResult.pageCount(),
                pages
        );
    }

    private LayoutPage analyzePage(ExtractedPage page) {
        String[] lines = page.text() == null ? new String[0] : page.text().split("\\R");

        List<LayoutBlock> blocks = new ArrayList<>();
        StringBuilder paragraphBuffer = new StringBuilder();
        int order = 1;
        String sectionTitle = null;

        for (String rawLine : lines) {
            String line = normalizeLine(rawLine);
            if (line.isBlank()) {
                if (!paragraphBuffer.isEmpty()) {
                    blocks.add(new LayoutBlock(order++, BlockType.PARAGRAPH, paragraphBuffer.toString().trim()));
                    paragraphBuffer.setLength(0);
                }
                continue;
            }

            BlockType type = classify(line);

            if (type == BlockType.SECTION_HEADER) {
                if (!paragraphBuffer.isEmpty()) {
                    blocks.add(new LayoutBlock(order++, BlockType.PARAGRAPH, paragraphBuffer.toString().trim()));
                    paragraphBuffer.setLength(0);
                }

                blocks.add(new LayoutBlock(order++, BlockType.SECTION_HEADER, line));
                if (sectionTitle == null) {
                    sectionTitle = line;
                }
                continue;
            }

            if (type == BlockType.BULLET || type == BlockType.TABLE_ROW) {
                if (!paragraphBuffer.isEmpty()) {
                    blocks.add(new LayoutBlock(order++, BlockType.PARAGRAPH, paragraphBuffer.toString().trim()));
                    paragraphBuffer.setLength(0);
                }

                blocks.add(new LayoutBlock(order++, type, line));
                continue;
            }

            if (paragraphBuffer.length() > 0) {
                paragraphBuffer.append(' ');
            }
            paragraphBuffer.append(line);
        }

        if (!paragraphBuffer.isEmpty()) {
            blocks.add(new LayoutBlock(order, BlockType.PARAGRAPH, paragraphBuffer.toString().trim()));
        }

        if (sectionTitle == null) {
            sectionTitle = "General";
        }

        return new LayoutPage(page.pageNumber(), sectionTitle, blocks);
    }

    private BlockType classify(String line) {
        if (isSectionHeader(line)) {
            return BlockType.SECTION_HEADER;
        }

        if (isBullet(line)) {
            return BlockType.BULLET;
        }

        if (isTableRow(line)) {
            return BlockType.TABLE_ROW;
        }

        if (!line.isBlank()) {
            return BlockType.PARAGRAPH;
        }

        return BlockType.UNKNOWN;
    }

    private boolean isSectionHeader(String line) {
        String trimmed = line.trim();

        if (trimmed.length() > 80) {
            return false;
        }

        boolean allUpper = trimmed.equals(trimmed.toUpperCase(Locale.ROOT))
                && trimmed.matches(".*[A-Z].*");

        boolean titleLike = trimmed.matches("^[A-Z][A-Za-z0-9/&(),\\- ]{2,79}$")
                && wordCount(trimmed) <= 8
                && !trimmed.endsWith(".")
                && !trimmed.contains(":");

        return allUpper || titleLike;
    }

    private boolean isBullet(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("- ")
                || trimmed.startsWith("* ")
                || trimmed.startsWith("• ")
                || trimmed.matches("^\\d+\\.\\s+.*")
                || trimmed.matches("^[a-zA-Z]\\)\\s+.*");
    }

    private boolean isTableRow(String line) {
        String trimmed = line.trim();

        if (trimmed.contains("|")) {
            return true;
        }

        return trimmed.matches(".*\\S\\s{2,}\\S.*");
    }

    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }

        return line.replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .trim();
    }

    private int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
