package com.nkeanyi.knowledge.controller;

import com.nkeanyi.knowledge.chunking.TextChunk;
import com.nkeanyi.knowledge.chunking.TextChunker;
import com.nkeanyi.knowledge.embedding.EmbeddingProvider;
import com.nkeanyi.knowledge.model.DocumentIngestionResponse;
import com.nkeanyi.knowledge.model.KnowledgeAnswerResponse;
import com.nkeanyi.knowledge.model.KnowledgeIndexRequest;
import com.nkeanyi.knowledge.model.KnowledgeIndexResponse;
import com.nkeanyi.knowledge.model.KnowledgeQueryRequest;
import com.nkeanyi.knowledge.model.KnowledgeQueryResponse;
import com.nkeanyi.knowledge.model.PackagedKnowledgeResponse;
import com.nkeanyi.knowledge.retrieval.KeywordScorer;
import com.nkeanyi.knowledge.service.KnowledgeAnswerService;
import com.nkeanyi.knowledge.service.KnowledgeQueryService;
import com.nkeanyi.knowledge.service.KnowledgeStorageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeQueryService knowledgeQueryService;
    private final KnowledgeStorageService knowledgeStorageService;
    private final KnowledgeAnswerService knowledgeAnswerService;
    private final EmbeddingProvider embeddingProvider;
    private final TextChunker textChunker;
    private final KeywordScorer keywordScorer;

    public KnowledgeController(
            KnowledgeQueryService knowledgeQueryService,
            KnowledgeStorageService knowledgeStorageService,
            KnowledgeAnswerService knowledgeAnswerService,
            EmbeddingProvider embeddingProvider,
            TextChunker textChunker,
            KeywordScorer keywordScorer
    ) {
        this.knowledgeQueryService = knowledgeQueryService;
        this.knowledgeStorageService = knowledgeStorageService;
        this.knowledgeAnswerService = knowledgeAnswerService;
        this.embeddingProvider = embeddingProvider;
        this.textChunker = textChunker;
        this.keywordScorer = keywordScorer;
    }

    @PostMapping("/index-text")
    public KnowledgeIndexResponse indexText(@Valid @RequestBody KnowledgeIndexRequest request) {
        return knowledgeQueryService.index(request.source(), request.content());
    }

    @PostMapping("/search")
    public KnowledgeQueryResponse search(@Valid @RequestBody KnowledgeQueryRequest request) {
        return knowledgeQueryService.search(request.query());
    }

    @PostMapping("/search-packaged")
    public PackagedKnowledgeResponse searchPackaged(@Valid @RequestBody KnowledgeQueryRequest request) {
        return knowledgeQueryService.searchWithCitations(request.query());
    }

    @PostMapping("/answer")
    public KnowledgeAnswerResponse answer(@Valid @RequestBody KnowledgeQueryRequest request) {
        return knowledgeAnswerService.answer(request.query());
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentIngestionResponse uploadAndIndex(@RequestPart("file") MultipartFile file) {
        return knowledgeStorageService.uploadAndIndex(file);
    }

    @PostMapping("/embedding-debug")
    public Map<String, Object> embeddingDebug(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "hello world");
        float[] vector = embeddingProvider.embed(text);
        return Map.of(
                "providerClass", embeddingProvider.getClass().getSimpleName(),
                "length", vector.length,
                "sample0", vector.length > 0 ? vector[0] : 0.0f
        );
    }

    @PostMapping("/chunk-debug")
    public Map<String, Object> chunkDebug(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        List<TextChunk> chunks = textChunker.chunk(text);
        return Map.of(
                "count", chunks.size(),
                "chunks", chunks
        );
    }

    @PostMapping("/keyword-debug")
    public Map<String, Object> keywordDebug(@RequestBody Map<String, String> body) {
        String query = body.getOrDefault("query", "");
        String content = body.getOrDefault("content", "");
        return Map.of(
                "query", query,
                "content", content,
                "keywordScore", keywordScorer.score(query, content)
        );
    }

    @GetMapping("/ping")
    public String ping() {
        return "knowledge-service-ok";
    }
}
