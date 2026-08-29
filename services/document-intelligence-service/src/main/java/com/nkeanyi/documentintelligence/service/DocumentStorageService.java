package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.config.DocumentIntelligenceProperties;
import com.nkeanyi.documentintelligence.model.DocumentUploadResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentStorageService {

    private final MinioClient minioClient;
    private final DocumentIntelligenceProperties properties;

    public DocumentStorageService(MinioClient minioClient, DocumentIntelligenceProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public DocumentUploadResponse upload(MultipartFile file) {
        try {
            String objectName = System.currentTimeMillis() + "-" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.minio().bucket())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return new DocumentUploadResponse(
                    objectName,
                    properties.minio().bucket(),
                    "UPLOADED"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document to MinIO", e);
        }
    }
}
