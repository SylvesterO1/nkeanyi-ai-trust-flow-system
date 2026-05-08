package com.nkeanyi.knowledge.citation;

import java.util.List;

public record CitationPackage(
        String query,
        List<Citation> citations,
        String assembledContext
) {}
