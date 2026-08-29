package com.nkeanyi.documentintelligence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nkeanyi.document-intelligence")
public record DocumentIntelligenceProperties(
        Minio minio,
        Knowledge knowledge,
        Ocr ocr
) {
    public record Minio(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket
    ) {}

    public record Knowledge(
            String baseUrl
    ) {}

    public record Ocr(
            boolean enabled,
            String tesseractCommand,
            int minExtractedChars
    ) {}
}
