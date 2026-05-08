package com.nkeanyi.documentintelligence.layout;

import java.util.List;

public record LayoutAnalysisResult(
        String fileName,
        String extractionMode,
        int pageCount,
        List<LayoutPage> pages
) {}
