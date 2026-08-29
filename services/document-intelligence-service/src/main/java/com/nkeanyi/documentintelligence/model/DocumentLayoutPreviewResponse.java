package com.nkeanyi.documentintelligence.model;

import com.nkeanyi.documentintelligence.layout.LayoutAnalysisResult;

public record DocumentLayoutPreviewResponse(
        String fileName,
        LayoutAnalysisResult layout
) {}
