package com.nkeanyi.documentintelligence.controller;

import com.nkeanyi.documentintelligence.model.DocumentAnalyzeRequest;
import com.nkeanyi.documentintelligence.model.DocumentAnalyzeResponse;
import com.nkeanyi.documentintelligence.model.DocumentLayoutPreviewResponse;
import com.nkeanyi.documentintelligence.model.DocumentProcessResponse;
import com.nkeanyi.documentintelligence.service.DocumentAnalysisService;
import com.nkeanyi.documentintelligence.service.DocumentLayoutPreviewService;
import com.nkeanyi.documentintelligence.service.DocumentProcessingService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentIntelligenceController {

    private final DocumentAnalysisService documentAnalysisService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentLayoutPreviewService documentLayoutPreviewService;

    public DocumentIntelligenceController(
            DocumentAnalysisService documentAnalysisService,
            DocumentProcessingService documentProcessingService,
            DocumentLayoutPreviewService documentLayoutPreviewService
    ) {
        this.documentAnalysisService = documentAnalysisService;
        this.documentProcessingService = documentProcessingService;
        this.documentLayoutPreviewService = documentLayoutPreviewService;
    }

    @PostMapping("/analyze")
    public DocumentAnalyzeResponse analyze(@Valid @RequestBody DocumentAnalyzeRequest request) {
        return documentAnalysisService.analyze(request.documentName(), request.documentType());
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentProcessResponse process(@RequestPart("file") MultipartFile file) {
        return documentProcessingService.process(file);
    }

    @PostMapping(value = "/layout-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentLayoutPreviewResponse layoutPreview(@RequestPart("file") MultipartFile file) {
        return documentLayoutPreviewService.preview(file);
    }

    @GetMapping("/ping")
    public String ping() {
        return "document-intelligence-service-ok";
    }
}
