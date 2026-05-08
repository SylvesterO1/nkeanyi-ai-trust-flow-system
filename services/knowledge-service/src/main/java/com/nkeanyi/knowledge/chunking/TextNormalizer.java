package com.nkeanyi.knowledge.chunking;

import org.springframework.stereotype.Component;

@Component
public class TextNormalizer {

    public String normalize(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\t', ' ')
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();

        return normalized;
    }
}
