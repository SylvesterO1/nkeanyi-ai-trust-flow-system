package com.nkeanyi.knowledge.kafka;

import com.nkeanyi.knowledge.model.KnowledgeIndexResponse;
import com.nkeanyi.knowledge.service.KnowledgeQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentExtractedConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractedConsumer.class);

    private final KnowledgeQueryService knowledgeQueryService;

    public DocumentExtractedConsumer(KnowledgeQueryService knowledgeQueryService) {
        this.knowledgeQueryService = knowledgeQueryService;
    }

    @KafkaListener(topics = "${DOCUMENT_EXTRACTED_TOPIC:document.extracted.v1}")
    public void onDocumentExtracted(DocumentExtractedEvent event) {
        log.info("Received DocumentExtractedEvent: {}", event);

        if (event == null) {
            log.warn("Received null DocumentExtractedEvent");
            return;
        }

        if (event.pages() != null && !event.pages().isEmpty()) {
            int chunkIndex = 0;

            for (DocumentExtractedEvent.PagePayload page : event.pages()) {
                if (page == null || page.text() == null || page.text().isBlank()) {
                    log.warn("Skipping empty page payload for source={}", event.objectName());
                    continue;
                }

                String pointId = UUID.randomUUID().toString();

                KnowledgeIndexResponse response = knowledgeQueryService.index(
                        pointId,
                        page.text(),
                        event.objectName(),
                        page.pageNumber(),
                        chunkIndex,
                        event.documentType(),
                        page.sectionTitle(),
                        event.extractorUsed(),
                        event.extractionMode(),
                        event.ocrUsed(),
                        event.confidence()
                );

                log.info("Indexed point successfully: {}", response);
                chunkIndex++;
            }

            return;
        }

        if (event.extractedText() != null && !event.extractedText().isBlank()) {
            String pointId = UUID.randomUUID().toString();

            KnowledgeIndexResponse response = knowledgeQueryService.index(
                    pointId,
                    event.extractedText(),
                    event.objectName(),
                    1,
                    0,
                    event.documentType(),
                    "UNKNOWN",
                    event.extractorUsed(),
                    event.extractionMode(),
                    event.ocrUsed(),
                    event.confidence()
            );

            log.info("Indexed fallback point successfully: {}", response);
        } else {
            log.warn("Event had no pages and no extractedText: source={}", event.objectName());
        }
    }
}
