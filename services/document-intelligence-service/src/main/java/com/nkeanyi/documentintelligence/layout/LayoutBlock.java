package com.nkeanyi.documentintelligence.layout;

public record LayoutBlock(
        int order,
        BlockType type,
        String text
) {}
