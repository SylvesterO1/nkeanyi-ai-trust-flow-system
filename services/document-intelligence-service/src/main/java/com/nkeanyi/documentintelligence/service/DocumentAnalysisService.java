package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.model.DocumentAnalyzeResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DocumentAnalysisService {

    public DocumentAnalyzeResponse analyze(String documentName, String documentType) {
        return new DocumentAnalyzeResponse(
                documentName,
                documentType,
                "ANALYZED",
                Map.of(
                        "documentName", documentName,
                        "documentType", documentType,
                        "summary", "Placeholder extraction result"
                )
        );
    }
}
