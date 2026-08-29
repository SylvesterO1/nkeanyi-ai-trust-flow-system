package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.entity.DocumentRecord;
import com.nkeanyi.documentintelligence.config.DocumentIntelligenceProperties;
import com.nkeanyi.documentintelligence.extract.ExtractionResult;
import com.nkeanyi.documentintelligence.extract.TextExtractionService;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractedEvent;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractionFailedEvent;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractedPublisher;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractionFailedPublisher;
import com.nkeanyi.documentintelligence.model.DocumentProcessResponse;
import com.nkeanyi.documentintelligence.model.DocumentRecordResponse;
import com.nkeanyi.documentintelligence.model.DocumentType;
import com.nkeanyi.documentintelligence.observability.RegulatedRequestContext;
import com.nkeanyi.documentintelligence.repository.DocumentRecordRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final MinioClient minioClient;
    private final DocumentIntelligenceProperties properties;
    private final TextExtractionService textExtractionService;
    private final DocumentExtractedPublisher documentExtractedPublisher;
    private final DocumentExtractionFailedPublisher documentExtractionFailedPublisher;
    private final DocumentClassificationService documentClassificationService;
    private final SectionTitleResolver sectionTitleResolver;
    private final DocumentRecordRepository documentRecordRepository;
    private final EnterpriseDocumentWorkflowService enterpriseDocumentWorkflowService;
    private final ConcurrentMap<String, DocumentProcessResponse> responsesByIdempotencyKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DocumentRecordResponse> recordsByDocumentId = new ConcurrentHashMap<>();

    @Autowired
    public DocumentProcessingService(
            MinioClient minioClient,
            DocumentIntelligenceProperties properties,
            TextExtractionService textExtractionService,
            DocumentExtractedPublisher documentExtractedPublisher,
            DocumentExtractionFailedPublisher documentExtractionFailedPublisher,
            DocumentClassificationService documentClassificationService,
            SectionTitleResolver sectionTitleResolver,
            DocumentRecordRepository documentRecordRepository,
            EnterpriseDocumentWorkflowService enterpriseDocumentWorkflowService
    ) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.textExtractionService = textExtractionService;
        this.documentExtractedPublisher = documentExtractedPublisher;
        this.documentExtractionFailedPublisher = documentExtractionFailedPublisher;
        this.documentClassificationService = documentClassificationService;
        this.sectionTitleResolver = sectionTitleResolver;
        this.documentRecordRepository = documentRecordRepository;
        this.enterpriseDocumentWorkflowService = enterpriseDocumentWorkflowService;
    }

    public DocumentProcessingService(
            MinioClient minioClient,
            DocumentIntelligenceProperties properties,
            TextExtractionService textExtractionService,
            DocumentExtractedPublisher documentExtractedPublisher,
            DocumentExtractionFailedPublisher documentExtractionFailedPublisher,
            DocumentClassificationService documentClassificationService,
            SectionTitleResolver sectionTitleResolver
    ) {
        this(
                minioClient,
                properties,
                textExtractionService,
                documentExtractedPublisher,
                documentExtractionFailedPublisher,
                documentClassificationService,
                sectionTitleResolver,
                null,
                null
        );
    }

    public DocumentProcessResponse process(MultipartFile file) {
        return process(file, RegulatedRequestContext.idempotencyKey());
    }

    public DocumentProcessResponse process(MultipartFile file, String idempotencyKey) {
        String safeIdempotencyKey = safeIdempotencyKey(idempotencyKey);
        DocumentProcessResponse existing = responsesByIdempotencyKey.get(safeIdempotencyKey);
        if (existing != null) {
            log.info("document_process_idempotency_replay idempotencyKey={} documentId={}",
                    safeIdempotencyKey,
                    existing.objectName()
            );
            return existing;
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-file";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String objectName = System.currentTimeMillis() + "-" + originalName;

        try {
            storeObject(file, objectName, contentType);

            byte[] bytes = file.getBytes();
            String checksum = sha256(bytes);
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
                            RegulatedRequestContext.tenantId(),
                            RegulatedRequestContext.correlationId(),
                            safeIdempotencyKey,
                            objectName,
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

            DocumentProcessResponse response = new DocumentProcessResponse(
                    objectName,
                    objectName,
                    properties.minio().bucket(),
                    RegulatedRequestContext.tenantId(),
                    RegulatedRequestContext.correlationId(),
                    originalName,
                    contentType,
                    extractionResult.text(),
                    extractionResult.totalCharacters(),
                    extractionResult.extractorUsed(),
                    extractionResult.ocrUsed(),
                    extractionResult.extractionMode(),
                    extractionResult.confidence(),
                    "EVENT_PUBLISHED",
                    "PROCESSED_AND_PUBLISHED"
            );
            responsesByIdempotencyKey.putIfAbsent(safeIdempotencyKey, response);
            saveRecord(new DocumentRecordResponse(
                    objectName,
                    RegulatedRequestContext.tenantId(),
                    RegulatedRequestContext.correlationId(),
                    originalName,
                    contentType,
                    extractionResult.extractorUsed(),
                    extractionResult.confidence(),
                    "PROCESSED",
                    null,
                    Instant.now(),
                    Instant.now(),
                    extractionResult.text()
            ));
            registerEnterpriseWorkflowSuccess(
                    objectName,
                    originalName,
                    contentType,
                    checksum,
                    extractionResult,
                    documentType
            );
            return response;
        } catch (Exception e) {
            saveRecord(new DocumentRecordResponse(
                    objectName,
                    RegulatedRequestContext.tenantId(),
                    RegulatedRequestContext.correlationId(),
                    originalName,
                    contentType,
                    null,
                    null,
                    "FAILED",
                    e.getMessage(),
                    Instant.now(),
                    Instant.now(),
                    null
            ));
            registerEnterpriseWorkflowFailure(objectName, originalName, contentType, e.getMessage());
            publishFailure(safeIdempotencyKey, file, e);
            throw new RuntimeException("Failed to process and publish document: " + e.getMessage(), e);
        }
    }

    protected void storeObject(MultipartFile file, String objectName, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(properties.minio().bucket())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(contentType)
                        .build()
        );
    }

    public List<DocumentRecordResponse> listRecords() {
        return listRecords(null, null, 25);
    }

    public List<DocumentRecordResponse> listRecords(String tenantId, String correlationId, int limit) {
        int safeLimit = safeLimit(limit);
        if (documentRecordRepository != null) {
            PageRequest pageRequest = PageRequest.of(0, safeLimit);
            if (hasText(tenantId) && hasText(correlationId)) {
                return documentRecordRepository
                        .findByTenantIdAndCorrelationIdOrderByCreatedAtDesc(tenantId, correlationId, pageRequest)
                        .stream()
                        .map(this::toResponseWithoutExtractedText)
                        .toList();
            }

            if (hasText(tenantId)) {
                return documentRecordRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageRequest)
                        .stream()
                        .map(this::toResponseWithoutExtractedText)
                        .toList();
            }

            if (hasText(correlationId)) {
                return documentRecordRepository.findByCorrelationIdOrderByCreatedAtDesc(correlationId, pageRequest)
                        .stream()
                        .map(this::toResponseWithoutExtractedText)
                        .toList();
            }

            return documentRecordRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                    .stream()
                    .map(this::toResponseWithoutExtractedText)
                    .toList();
        }

        return recordsByDocumentId.values().stream()
                .filter(record -> !hasText(tenantId) || tenantId.equals(record.tenantId()))
                .filter(record -> !hasText(correlationId) || correlationId.equals(record.correlationId()))
                .sorted(Comparator.comparing(DocumentRecordResponse::createdAt).reversed())
                .limit(safeLimit)
                .map(DocumentRecordResponse::withoutExtractedText)
                .toList();
    }

    public Optional<DocumentRecordResponse> findRecordByDocumentId(String documentId) {
        if (documentRecordRepository != null) {
            return documentRecordRepository.findByDocumentId(documentId).map(this::toResponse);
        }
        return Optional.ofNullable(recordsByDocumentId.get(documentId));
    }

    public List<DocumentRecordResponse> findRecordsByCorrelationId(String correlationId) {
        return findRecordsByCorrelationId(correlationId, null, 25);
    }

    public List<DocumentRecordResponse> findRecordsByCorrelationId(String correlationId, String tenantId, int limit) {
        return listRecords(tenantId, correlationId, limit);
    }

    private void saveRecord(DocumentRecordResponse response) {
        recordsByDocumentId.putIfAbsent(response.documentId(), response);
        if (documentRecordRepository == null) {
            return;
        }

        try {
            documentRecordRepository.save(new DocumentRecord(
                    response.documentId(),
                    response.tenantId(),
                    response.correlationId(),
                    response.originalFilename(),
                    response.contentType(),
                    response.extractorUsed(),
                    response.confidence(),
                    response.status(),
                    response.extractedText(),
                    response.errorMessage(),
                    response.createdAt(),
                    response.updatedAt()
            ));
        } catch (Exception exception) {
            log.warn("document_record_persist_failed documentId={} error={}",
                    response.documentId(),
                    exception.getMessage());
        }
    }

    private DocumentRecordResponse toResponseWithoutExtractedText(DocumentRecord record) {
        return toResponse(record).withoutExtractedText();
    }

    private DocumentRecordResponse toResponse(DocumentRecord record) {
        return new DocumentRecordResponse(
                record.documentId(),
                record.tenantId(),
                record.correlationId(),
                record.originalFilename(),
                record.contentType(),
                record.extractorUsed(),
                record.confidence(),
                record.status(),
                record.errorMessage(),
                record.createdAt(),
                record.updatedAt(),
                record.extractedText()
        );
    }

    private void registerEnterpriseWorkflowSuccess(
            String documentId,
            String originalName,
            String contentType,
            String checksum,
            ExtractionResult extractionResult,
            DocumentType documentType
    ) {
        if (enterpriseDocumentWorkflowService == null) {
            return;
        }

        try {
            enterpriseDocumentWorkflowService.registerProcessedDocument(
                    new EnterpriseDocumentWorkflowService.RegisterProcessedDocumentCommand(
                            documentId,
                            RegulatedRequestContext.tenantId(),
                            "default-workspace",
                            RegulatedRequestContext.correlationId(),
                            "minio://" + properties.minio().bucket() + "/" + documentId,
                            checksum,
                            "unassigned-owner",
                            originalName,
                            contentType,
                            documentType.name(),
                            extractionResult.extractorUsed(),
                            extractionResult.ocrUsed(),
                            extractionResult.confidence(),
                            extractionResult.text(),
                            "standard-7-years"
                    )
            );
        } catch (Exception exception) {
            log.warn("enterprise_document_workflow_register_failed documentId={} error={}",
                    documentId,
                    exception.getMessage());
        }
    }

    private void registerEnterpriseWorkflowFailure(
            String documentId,
            String originalName,
            String contentType,
            String errorMessage
    ) {
        if (enterpriseDocumentWorkflowService == null) {
            return;
        }

        try {
            enterpriseDocumentWorkflowService.registerFailedDocument(
                    new EnterpriseDocumentWorkflowService.RegisterFailedDocumentCommand(
                            documentId,
                            RegulatedRequestContext.tenantId(),
                            "default-workspace",
                            RegulatedRequestContext.correlationId(),
                            "minio://" + properties.minio().bucket() + "/" + documentId,
                            null,
                            "unassigned-owner",
                            originalName,
                            contentType,
                            errorMessage,
                            "standard-7-years"
                    )
            );
        } catch (Exception exception) {
            log.warn("enterprise_document_workflow_failure_register_failed documentId={} error={}",
                    documentId,
                    exception.getMessage());
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private int safeLimit(int limit) {
        if (limit <= 0) {
            return 25;
        }
        return Math.min(limit, 100);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void publishFailure(String idempotencyKey, MultipartFile file, Exception exception) {
        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-file";
            documentExtractionFailedPublisher.publish(new DocumentExtractionFailedEvent(
                    RegulatedRequestContext.tenantId(),
                    RegulatedRequestContext.correlationId(),
                    idempotencyKey,
                    originalName,
                    exception.getClass().getSimpleName(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    false,
                    "document-intelligence-service",
                    Instant.now()
            ));
        } catch (Exception ignored) {
            log.warn("document_extraction_failed_event_publish_failed idempotencyKey={}", idempotencyKey);
        }
    }

    private String safeIdempotencyKey(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? RegulatedRequestContext.idempotencyKey()
                : idempotencyKey;
    }

}
