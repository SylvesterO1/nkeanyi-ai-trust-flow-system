package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.config.DocumentIntelligenceProperties;
import com.nkeanyi.documentintelligence.extract.ExtractedPage;
import com.nkeanyi.documentintelligence.extract.ExtractionResult;
import com.nkeanyi.documentintelligence.extract.TextExtractionService;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractedEvent;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractedPublisher;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractionFailedEvent;
import com.nkeanyi.documentintelligence.kafka.DocumentExtractionFailedPublisher;
import com.nkeanyi.documentintelligence.observability.RegulatedRequestContext;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentProcessingServiceFailureTest {

    @Test
    void extractorFailurePublishesDocumentExtractionFailedEvidenceEvent() {
        CapturingFailurePublisher failurePublisher = new CapturingFailurePublisher(false);
        DocumentProcessingService service = service(
                new FailingExtractionService(),
                new CapturingExtractedPublisher(false),
                failurePublisher,
                false
        );

        assertThatThrownBy(() -> service.process(file(), "idem-doc-fail"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process and publish document");

        assertThat(failurePublisher.events).hasSize(1);
        assertThat(failurePublisher.events.get(0).idempotencyKey()).isEqualTo("idem-doc-fail");
        assertThat(failurePublisher.events.get(0).errorType()).isEqualTo("IllegalStateException");
        assertThat(failurePublisher.events.get(0).errorCode()).isEqualTo("IllegalStateException");
        assertThat(failurePublisher.events.get(0).recoverable()).isFalse();
        assertThat(failurePublisher.events.get(0).sourceService()).isEqualTo("document-intelligence-service");
        assertThat(failurePublisher.events.get(0).timestamp()).isNotNull();
    }

    @Test
    void storageFailurePublishesDocumentExtractionFailedEvidenceEvent() {
        CapturingFailurePublisher failurePublisher = new CapturingFailurePublisher(false);
        DocumentProcessingService service = service(
                new SuccessfulExtractionService(),
                new CapturingExtractedPublisher(false),
                failurePublisher,
                true
        );

        assertThatThrownBy(() -> service.process(file(), "idem-storage-fail"))
                .isInstanceOf(RuntimeException.class);

        assertThat(failurePublisher.events).hasSize(1);
        assertThat(failurePublisher.events.get(0).errorMessage()).contains("storage unavailable");
    }

    @Test
    void documentExtractedPublishFailurePublishesFailureEvidenceEvent() {
        CapturingFailurePublisher failurePublisher = new CapturingFailurePublisher(false);
        DocumentProcessingService service = service(
                new SuccessfulExtractionService(),
                new CapturingExtractedPublisher(true),
                failurePublisher,
                false
        );

        assertThatThrownBy(() -> service.process(file(), "idem-publish-fail"))
                .isInstanceOf(RuntimeException.class);

        assertThat(failurePublisher.events).hasSize(1);
        assertThat(failurePublisher.events.get(0).errorMessage()).contains("document extracted publish failed");
    }

    @Test
    void duplicateIdempotencyKeyDoesNotRepublishDocumentExtractedEvent() {
        CapturingExtractedPublisher extractedPublisher = new CapturingExtractedPublisher(false);
        DocumentProcessingService service = service(
                new SuccessfulExtractionService(),
                extractedPublisher,
                new CapturingFailurePublisher(false),
                false
        );

        var first = service.process(file(), "idem-repeat");
        var second = service.process(file(), "idem-repeat");

        assertThat(second).isEqualTo(first);
        assertThat(extractedPublisher.events).hasSize(1);
    }

    @Test
    void processedDocumentsAreVisibleByListDocumentIdAndCorrelationId() {
        DocumentProcessingService service = service(
                new SuccessfulExtractionService(),
                new CapturingExtractedPublisher(false),
                new CapturingFailurePublisher(false),
                false
        );

        MDC.put(RegulatedRequestContext.MDC_TENANT_ID, "tenant-record-test");
        MDC.put(RegulatedRequestContext.MDC_CORRELATION_ID, "corr-record-test");
        try {
            var response = service.process(file(), "idem-record-test");

            assertThat(service.listRecords()).hasSize(1);
            assertThat(service.listRecords().get(0).documentId()).isEqualTo(response.objectName());
            assertThat(service.listRecords().get(0).tenantId()).isEqualTo("tenant-record-test");
            assertThat(service.listRecords().get(0).correlationId()).isEqualTo("corr-record-test");
            assertThat(service.listRecords().get(0).originalFilename()).isEqualTo("policy.txt");
            assertThat(service.listRecords().get(0).contentType()).isEqualTo("text/plain");
            assertThat(service.listRecords().get(0).extractorUsed()).isEqualTo("FakeExtractor");
            assertThat(service.listRecords().get(0).confidence()).isEqualTo(1.0);
            assertThat(service.listRecords().get(0).status()).isEqualTo("PROCESSED");
            assertThat(service.listRecords().get(0).createdAt()).isNotNull();
            assertThat(service.listRecords().get(0).updatedAt()).isNotNull();
            assertThat(service.listRecords().get(0).extractedText()).isNull();

            assertThat(service.findRecordByDocumentId(response.objectName()))
                    .hasValueSatisfying(record -> assertThat(record.extractedText())
                            .contains("Policy text requires sanctions screening"));
            assertThat(service.findRecordsByCorrelationId("corr-record-test"))
                    .extracting("documentId")
                    .containsExactly(response.objectName());
        } finally {
            MDC.clear();
        }
    }

    private DocumentProcessingService service(
            TextExtractionService extractionService,
            DocumentExtractedPublisher extractedPublisher,
            DocumentExtractionFailedPublisher failurePublisher,
            boolean failStorage
    ) {
        return new DocumentProcessingService(
                null,
                properties(),
                extractionService,
                extractedPublisher,
                failurePublisher,
                new DocumentClassificationService(),
                new SectionTitleResolver()
        ) {
            @Override
            protected void storeObject(MultipartFile file, String objectName, String contentType) throws Exception {
                if (failStorage) {
                    throw new IllegalStateException("storage unavailable");
                }
            }
        };
    }

    private MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "policy.txt",
                "text/plain",
                "Policy text requires sanctions screening before activation.".getBytes()
        );
    }

    private DocumentIntelligenceProperties properties() {
        return new DocumentIntelligenceProperties(
                new DocumentIntelligenceProperties.Minio("http://localhost:9000", "access", "secret", "documents"),
                new DocumentIntelligenceProperties.Knowledge("http://localhost:8088"),
                new DocumentIntelligenceProperties.Ocr(false, "tesseract", 40)
        );
    }

    private class SuccessfulExtractionService extends TextExtractionService {
        SuccessfulExtractionService() {
            super(List.of(), null, properties());
        }

        @Override
        public ExtractionResult extract(byte[] content, String contentType, String fileName) {
            String text = "Policy text requires sanctions screening before activation.";
            return new ExtractionResult(
                    text,
                    "FakeExtractor",
                    "PARSED_TEXT",
                    false,
                    text.length(),
                    1,
                    1.0,
                    List.of(new ExtractedPage(1, text, text.length()))
            );
        }
    }

    private class FailingExtractionService extends TextExtractionService {
        FailingExtractionService() {
            super(List.of(), null, properties());
        }

        @Override
        public ExtractionResult extract(byte[] content, String contentType, String fileName) {
            throw new IllegalStateException("extractor unavailable");
        }
    }

    private static class CapturingExtractedPublisher extends DocumentExtractedPublisher {
        private final boolean fail;
        private final java.util.List<DocumentExtractedEvent> events = new java.util.ArrayList<>();

        CapturingExtractedPublisher(boolean fail) {
            super(null, "document.extracted.v1");
            this.fail = fail;
        }

        @Override
        public void publish(DocumentExtractedEvent event) {
            if (fail) {
                throw new IllegalStateException("document extracted publish failed");
            }
            events.add(event);
        }
    }

    private static class CapturingFailurePublisher extends DocumentExtractionFailedPublisher {
        private final boolean fail;
        private final java.util.List<DocumentExtractionFailedEvent> events = new java.util.ArrayList<>();

        CapturingFailurePublisher(boolean fail) {
            super(null, "document.extraction.failed.v1");
            this.fail = fail;
        }

        @Override
        public void publish(DocumentExtractionFailedEvent event) {
            if (fail) {
                throw new IllegalStateException("failure publish failed");
            }
            events.add(event);
        }
    }
}
