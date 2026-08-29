package com.nkeanyi.knowledge.kafka;

import com.nkeanyi.knowledge.model.KnowledgeIndexResponse;
import com.nkeanyi.knowledge.observability.KafkaMdcContext;
import com.nkeanyi.knowledge.service.KnowledgeStorageService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentExtractedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractedConsumer.class);

    private final KnowledgeStorageService knowledgeStorageService;

    public DocumentExtractedConsumer(KnowledgeStorageService knowledgeStorageService) {
        this.knowledgeStorageService = knowledgeStorageService;
    }

    @KafkaListener(topics = "${DOCUMENT_EXTRACTED_TOPIC:document.extracted.v1}")
    public void onDocumentExtracted(ConsumerRecord<String, DocumentExtractedEvent> record) {
        KafkaMdcContext.runWithKafkaHeaders(record.headers(), () -> {
            DocumentExtractedEvent event = record.value();

            log.info(
                    "document_extracted_event_received topic={} partition={} offset={} key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );

            if (event == null) {
                log.warn("received_null_document_extracted_event");
                return;
            }

            log.info("processing_document_extracted_event source={} documentType={}",
                    event.objectName(),
                    event.documentType()
            );

            if (event.pages() != null && !event.pages().isEmpty()) {
                for (DocumentExtractedEvent.PagePayload page : event.pages()) {
                    if (page == null || page.text() == null || page.text().isBlank()) {
                        log.warn("skipping_empty_page_payload source={}", event.objectName());
                        continue;
                    }

                    List<KnowledgeIndexResponse> responses = knowledgeStorageService.store(
                            documentId(event),
                            event.objectName(),
                            page.text(),
                            page.pageNumber(),
                            event.documentType(),
                            page.sectionTitle(),
                            event.extractorUsed(),
                            event.extractionMode(),
                            event.ocrUsed(),
                            event.confidence()
                    );

                    log.info(
                            "indexed_document_page_successfully documentId={} source={} pageNumber={} chunksIndexed={}",
                            documentId(event),
                            event.objectName(),
                            page.pageNumber(),
                            responses.size()
                    );
                }

                return;
            }

            if (event.extractedText() != null && !event.extractedText().isBlank()) {
                List<KnowledgeIndexResponse> responses = knowledgeStorageService.store(
                        documentId(event),
                        event.objectName(),
                        event.extractedText(),
                        1,
                        event.documentType(),
                        "UNKNOWN",
                        event.extractorUsed(),
                        event.extractionMode(),
                        event.ocrUsed(),
                        event.confidence()
                );

                log.info(
                        "indexed_fallback_document_text_successfully documentId={} source={} chunksIndexed={}",
                        documentId(event),
                        event.objectName(),
                        responses.size()
                );
            } else {
                log.warn("document_extracted_event_has_no_pages_or_text source={}", event.objectName());
            }
        });
    }

    private String documentId(DocumentExtractedEvent event) {
        if (event.documentId() != null && !event.documentId().isBlank()) {
            return event.documentId();
        }

        return event.objectName();
    }
}
