package com.nkeanyi.knowledge.service;

import com.nkeanyi.knowledge.chunking.TextChunk;
import com.nkeanyi.knowledge.chunking.TextChunker;
import com.nkeanyi.knowledge.model.DocumentIngestionResponse;
import com.nkeanyi.knowledge.model.KnowledgeIndexResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeStorageService {

    private static final String DIRECT_UPLOAD_BUCKET = "knowledge-direct-upload";

    private final TextChunker textChunker;
    private final KnowledgeQueryService knowledgeQueryService;

    public KnowledgeStorageService(
            TextChunker textChunker,
            KnowledgeQueryService knowledgeQueryService
    ) {
        this.textChunker = textChunker;
        this.knowledgeQueryService = knowledgeQueryService;
    }

    public DocumentIngestionResponse uploadAndIndex(MultipartFile file) {
        try {
            String objectName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "uploaded-file";

            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            List<KnowledgeIndexResponse> indexed = store(
                    objectName,
                    content,
                    1,
                    "GENERAL",
                    "UNKNOWN",
                    "DirectUpload",
                    "PARSED",
                    false,
                    1.0
            );

            return new DocumentIngestionResponse(
                    objectName,
                    DIRECT_UPLOAD_BUCKET,
                    indexed.size(),
                    "INDEXED"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload and index file", e);
        }
    }

    public List<KnowledgeIndexResponse> store(String source, String content) {
        return store(source, content, 1, "GENERAL", "UNKNOWN", "DirectUpload", "PARSED", false, 1.0);
    }

    public List<KnowledgeIndexResponse> store(
            String source,
            String content,
            int pageNumber,
            String documentType,
            String sectionTitle,
            String extractorUsed,
            String extractionMode,
            boolean ocrUsed,
            double confidence
    ) {
        List<TextChunk> chunks = textChunker.chunk(content);

        return chunks.stream()
                .map(chunk -> knowledgeQueryService.index(
                        UUID.randomUUID().toString(),
                        chunk.content(),
                        source,
                        pageNumber,
                        chunk.index(),
                        documentType,
                        sectionTitle,
                        extractorUsed,
                        extractionMode,
                        ocrUsed,
                        confidence
                ))
                .toList();
    }
}
