package com.nkeanyi.knowledge.extract;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TextExtractionService {

    private final List<TextExtractor> extractors;

    public TextExtractionService(List<TextExtractor> extractors) {
        this.extractors = extractors;
    }

    public String extract(byte[] content, String contentType, String fileName) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(contentType, fileName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type for extraction: " + fileName))
                .extract(content, fileName);
    }
}
