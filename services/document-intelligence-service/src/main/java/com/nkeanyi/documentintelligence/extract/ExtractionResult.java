package com.nkeanyi.documentintelligence.extract;

import java.util.List;

public record ExtractionResult(
        String text,
        String extractorUsed,
        String extractionMode,
        boolean ocrUsed,
        int totalCharacters,
        int pageCount,
        double confidence,
        List<ExtractedPage> pages
) {}
