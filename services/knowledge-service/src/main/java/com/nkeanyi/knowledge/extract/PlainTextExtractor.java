package com.nkeanyi.knowledge.extract;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PlainTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null && contentType.startsWith("text/")) {
            return true;
        }
        return fileName != null && (
                fileName.endsWith(".txt") ||
                fileName.endsWith(".md") ||
                fileName.endsWith(".csv") ||
                fileName.endsWith(".log")
        );
    }

    @Override
    public String extract(byte[] content, String fileName) {
        return new String(content, StandardCharsets.UTF_8);
    }
}
