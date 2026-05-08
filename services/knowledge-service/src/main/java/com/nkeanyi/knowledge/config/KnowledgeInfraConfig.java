package com.nkeanyi.knowledge.config;

import io.minio.MinioClient;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeInfraConfig {

    @Bean
    MinioClient minioClient(KnowledgeServiceProperties properties) {
        var cfg = properties.minio();
        if (cfg == null) {
            throw new IllegalStateException("Missing configuration: nkeanyi.knowledge.minio");
        }

        return MinioClient.builder()
                .endpoint(cfg.endpoint())
                .credentials(cfg.accessKey(), cfg.secretKey())
                .build();
    }

    @Bean
    QdrantClient qdrantClient(KnowledgeServiceProperties properties) {
        var cfg = properties.qdrant();
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(cfg.host(), cfg.port(), false).build()
        );
    }
}
