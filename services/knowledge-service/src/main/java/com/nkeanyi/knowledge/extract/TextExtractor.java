package com.nkeanyi.knowledge.extract;

public interface TextExtractor {
    boolean supports(String contentType, String fileName);
    String extract(byte[] content, String fileName);
}
