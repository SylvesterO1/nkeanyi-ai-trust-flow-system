package com.nkeanyi.documentintelligence.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentInfraConfig {

    @Bean
    MinioClient minioClient(DocumentIntelligenceProperties properties) throws Exception {
        var cfg = properties.minio();
        MinioClient client = MinioClient.builder()
                .endpoint(cfg.endpoint())
                .credentials(cfg.accessKey(), cfg.secretKey())
                .build();

        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(cfg.bucket()).build()
        );

        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(cfg.bucket()).build());
        }

        return client;
    }
}
