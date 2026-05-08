package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.config.DocumentIntelligenceProperties;
import com.nkeanyi.documentintelligence.extract.ExtractionResult;
import com.nkeanyi.documentintelligence.extract.TextExtractionService;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractedEvent;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractedPublisher;
import com.nkeanyi.documentintelligence.model.DocumentProcessResponse;
import com.nkeanyi.documentintelligence.model.DocumentType;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final MinioClient minioClient;
    private final DocumentIntelligenceProperties properties;
    private final TextExtractionService textExtractionService;
    private final DocumentExtractedPublisher documentExtractedPublisher;
    private final DocumentClassificationService documentClassificationService;
    private final SectionTitleResolver sectionTitleResolver;

    public DocumentProcessingService(
            MinioClient minioClient,
            DocumentIntelligenceProperties properties,
            TextExtractionService textExtractionService,
            DocumentExtractedPublisher documentExtractedPublisher,
            DocumentClassificationService documentClassificationService,
            SectionTitleResolver sectionTitleResolver
    ) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.textExtractionService = textExtractionService;
        this.documentExtractedPublisher = documentExtractedPublisher;
        this.documentClassificationService = documentClassificationService;
        this.sectionTitleResolver = sectionTitleResolver;
    }

    public DocumentProcessResponse process(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-file";
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String objectName = System.currentTimeMillis() + "-" + originalName;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.minio().bucket())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            byte[] bytes = file.getBytes();
            ExtractionResult extractionResult = textExtractionService.extract(bytes, contentType, originalName);
            DocumentType documentType = documentClassificationService.classify(originalName, extractionResult.text());

            log.info(
                    "Document processed: objectName={}, documentType={}, extractorUsed={}, extractionMode={}, ocrUsed={}, totalCharacters={}, confidence={}, pageCount={}",
                    objectName,
                    documentType,
                    extractionResult.extractorUsed(),
                    extractionResult.extractionMode(),
                    extractionResult.ocrUsed(),
                    extractionResult.totalCharacters(),
                    extractionResult.confidence(),
                    extractionResult.pageCount()
            );

            List<DocumentExtractedEvent.PagePayload> pages = new ArrayList<>();

            extractionResult.pages().forEach(page -> {
                List<SectionTitleResolver.SectionBlock> blocks = sectionTitleResolver.splitIntoSections(page.text());

                if (blocks.isEmpty()) {
                    pages.add(new DocumentExtractedEvent.PagePayload(
                            page.pageNumber(),
                            "UNKNOWN",
                            page.text(),
                            page.charCount()
                    ));
                } else {
                    for (SectionTitleResolver.SectionBlock block : blocks) {
                        pages.add(new DocumentExtractedEvent.PagePayload(
                                page.pageNumber(),
                                block.sectionTitle(),
                                block.content(),
                                block.content().length()
                        ));
                    }
                }
            });

            documentExtractedPublisher.publish(
                    new DocumentExtractedEvent(
                            objectName,
                            properties.minio().bucket(),
                            contentType,
                            extractionResult.text(),
                            documentType.name(),
                            extractionResult.extractorUsed(),
                            extractionResult.extractionMode(),
                            extractionResult.ocrUsed(),
                            extractionResult.confidence(),
                            pages
                    )
            );

            return new DocumentProcessResponse(
                    objectName,
                    properties.minio().bucket(),
                    extractionResult.text(),
                    extractionResult.totalCharacters(),
                    extractionResult.extractorUsed(),
                    extractionResult.ocrUsed(),
                    extractionResult.extractionMode(),
                    extractionResult.confidence(),
                    "EVENT_PUBLISHED",
                    "PROCESSED_AND_PUBLISHED"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to process and publish document: " + e.getMessage(), e);
        }
    }
}
