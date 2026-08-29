package com.nkeanyi.knowledge.chunking;

public record TextChunk(
        int index,
        String content,
        int startOffset,
        int endOffset,
        int charCount
) {}
