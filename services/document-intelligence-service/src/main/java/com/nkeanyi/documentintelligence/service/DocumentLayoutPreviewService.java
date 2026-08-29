package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.extract.ExtractionResult;
import com.nkeanyi.documentintelligence.extract.TextExtractionService;
import com.nkeanyi.documentintelligence.layout.LayoutAnalysisResult;
import com.nkeanyi.documentintelligence.layout.LayoutAnalysisService;
import com.nkeanyi.documentintelligence.model.DocumentLayoutPreviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentLayoutPreviewService {

    private final TextExtractionService textExtractionService;
    private final LayoutAnalysisService layoutAnalysisService;

    public DocumentLayoutPreviewService(
            TextExtractionService textExtractionService,
            LayoutAnalysisService layoutAnalysisService
    ) {
        this.textExtractionService = textExtractionService;
        this.layoutAnalysisService = layoutAnalysisService;
    }

    public DocumentLayoutPreviewResponse preview(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded-file";
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            ExtractionResult extractionResult = textExtractionService.extract(
                    file.getBytes(),
                    contentType,
                    originalName
            );

            LayoutAnalysisResult layout = layoutAnalysisService.analyze(originalName, extractionResult);

            return new DocumentLayoutPreviewResponse(originalName, layout);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate layout preview: " + e.getMessage(), e);
        }
    }
}
