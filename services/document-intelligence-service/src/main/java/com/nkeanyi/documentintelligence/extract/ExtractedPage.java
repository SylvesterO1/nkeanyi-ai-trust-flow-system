package com.nkeanyi.documentintelligence.extract;

public record ExtractedPage(
        int pageNumber,
        String text,
        int charCount
) {}
