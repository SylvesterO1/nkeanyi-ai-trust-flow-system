package com.nkeanyi.knowledge.chunking;

import com.nkeanyi.knowledge.config.KnowledgeServiceProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final int chunkSize;
    private final int chunkOverlap;
    private final int minChunkSize;
    private final TextNormalizer textNormalizer;

    public TextChunker(KnowledgeServiceProperties properties, TextNormalizer textNormalizer) {
        this.chunkSize = properties.chunking().chunkSize();
        this.chunkOverlap = properties.chunking().chunkOverlap();
        this.minChunkSize = properties.chunking().minChunkSize();
        this.textNormalizer = textNormalizer;
    }

    public List<TextChunk> chunk(String rawText) {
        String text = textNormalizer.normalize(rawText);
        if (text.isBlank()) {
            return List.of();
        }

        List<String> units = splitIntoSemanticUnits(text);
        List<TextChunk> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        int chunkIndex = 0;
        int runningOffset = 0;
        int chunkStart = 0;

        for (String unit : units) {
            if (current.isEmpty()) {
                chunkStart = runningOffset;
            }

            if (current.length() + unit.length() <= chunkSize) {
                current.append(unit);
            } else {
                if (!current.isEmpty()) {
                    String chunkText = current.toString().trim();
                    if (chunkText.length() >= minChunkSize) {
                        chunks.add(new TextChunk(
                                chunkIndex++,
                                chunkText,
                                chunkStart,
                                chunkStart + chunkText.length(),
                                chunkText.length()
                        ));
                    }

                    String overlapSeed = tail(chunkText, chunkOverlap);
                    current.setLength(0);
                    if (!overlapSeed.isBlank()) {
                        current.append(overlapSeed).append("\n");
                    }
                }

                if (unit.length() > chunkSize) {
                    List<String> hardSplits = hardSplit(unit, chunkSize, chunkOverlap);
                    for (String split : hardSplits) {
                        String chunkText = split.trim();
                        if (chunkText.length() >= minChunkSize) {
                            chunks.add(new TextChunk(
                                    chunkIndex++,
                                    chunkText,
                                    runningOffset,
                                    runningOffset + chunkText.length(),
                                    chunkText.length()
                            ));
                        }
                    }
                    current.setLength(0);
                } else {
                    current.append(unit);
                }
            }

            runningOffset += unit.length();
        }

        if (!current.isEmpty()) {
            String chunkText = current.toString().trim();
            if (chunkText.length() >= minChunkSize) {
                chunks.add(new TextChunk(
                        chunkIndex,
                        chunkText,
                        chunkStart,
                        chunkStart + chunkText.length(),
                        chunkText.length()
                ));
            }
        }

        return chunks;
    }

    private List<String> splitIntoSemanticUnits(String text) {
        String[] paragraphs = text.split("\\n\\n+");
        List<String> units = new ArrayList<>();

        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isBlank()) {
                continue;
            }

            if (p.length() <= chunkSize) {
                units.add(p + "\n\n");
            } else {
                units.addAll(splitParagraphIntoSentences(p));
            }
        }

        return units;
    }

    private List<String> splitParagraphIntoSentences(String paragraph) {
        List<String> sentences = new ArrayList<>();
        String[] parts = paragraph.split("(?<=[.!?])\\s+");

        StringBuilder buffer = new StringBuilder();
        for (String part : parts) {
            if (buffer.length() + part.length() + 1 <= chunkSize) {
                if (!buffer.isEmpty()) {
                    buffer.append(' ');
                }
                buffer.append(part);
            } else {
                if (!buffer.isEmpty()) {
                    sentences.add(buffer.toString().trim() + "\n\n");
                    buffer.setLength(0);
                }

                if (part.length() > chunkSize) {
                    sentences.addAll(hardSplit(part, chunkSize, chunkOverlap));
                } else {
                    buffer.append(part);
                }
            }
        }

        if (!buffer.isEmpty()) {
            sentences.add(buffer.toString().trim() + "\n\n");
        }

        return sentences;
    }

    private List<String> hardSplit(String text, int maxSize, int overlap) {
        List<String> splits = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            String piece = text.substring(start, end).trim();
            if (!piece.isBlank()) {
                splits.add(piece);
            }

            if (end == text.length()) {
                break;
            }

            start = Math.max(end - overlap, start + 1);
        }

        return splits;
    }

    private String tail(String text, int overlap) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= overlap) {
            return text;
        }
        return text.substring(text.length() - overlap);
    }
}
