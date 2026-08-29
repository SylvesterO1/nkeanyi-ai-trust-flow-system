package com.nkeanyi.documentintelligence.layout;

import java.util.List;

public record LayoutPage(
        int pageNumber,
        String sectionTitle,
        List<LayoutBlock> blocks
) {}
