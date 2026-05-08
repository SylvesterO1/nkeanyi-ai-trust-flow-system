package com.nkeanyi.documentintelligence.extract;

import com.nkeanyi.documentintelligence.config.DocumentIntelligenceProperties;
import com.nkeanyi.documentintelligence.ocr.OcrService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TextExtractionService {

    private final List<TextExtractor> extractors;
    private final OcrService ocrService;
    private final DocumentIntelligenceProperties properties;

    public TextExtractionService(
            List<TextExtractor> extractors,
            OcrService ocrService,
            DocumentIntelligenceProperties properties
    ) {
        this.extractors = extractors;
        this.ocrService = ocrService;
        this.properties = properties;
    }

    public ExtractionResult extract(byte[] content, String contentType, String fileName) {
        TextExtractor extractor = extractors.stream()
                .filter(candidate -> candidate.supports(contentType, fileName))
                .findFirst()
                .orElse(null);

        String extracted = "";
        String extractorUsed = "NONE";
        String extractionMode = "NONE";
        boolean ocrUsed = false;
        int pageCount = 1;
        double confidence = 0.0;
        List<ExtractedPage> pages = List.of(new ExtractedPage(1, "", 0));

        if (extractor != null) {
            extracted = extractor.extract(content, fileName);
            extractorUsed = extractor.getClass().getSimpleName();
            extractionMode = "PARSED_TEXT";
            confidence = estimateParsedTextConfidence(extracted);

            if (extractor instanceof PdfTextExtractor pdfTextExtractor) {
                pages = pdfTextExtractor.extractPages(content, fileName);
                pageCount = pages.size();
            } else {
                extracted = normalize(extracted);
                pages = List.of(new ExtractedPage(1, extracted, extracted.length()));
                pageCount = 1;
            }
        }

        if (extracted == null) {
            extracted = "";
        }

        if (shouldFallbackToOcr(extracted, contentType, fileName)) {
            String ocrText = ocrService.extractWithOcr(content, contentType, fileName);

            if (ocrText != null && !ocrText.isBlank()) {
                extracted = normalize(ocrText);
                extractorUsed = "OCR_FALLBACK";
                extractionMode = "OCR";
                ocrUsed = true;
                confidence = estimateOcrConfidence(extracted);
                pages = List.of(new ExtractedPage(1, extracted, extracted.length()));
                pageCount = 1;
            }
        }

        if (extracted.isBlank()) {
            throw new IllegalStateException("No text extracted from file: " + fileName);
        }

        return new ExtractionResult(
                extracted,
                extractorUsed,
                extractionMode,
                ocrUsed,
                extracted.length(),
                pageCount,
                confidence,
                pages
        );
    }

    private boolean shouldFallbackToOcr(String extracted, String contentType, String fileName) {
        if (extracted == null) {
            return true;
        }

        boolean isPdf = "application/pdf".equalsIgnoreCase(contentType)
                || (fileName != null && fileName.toLowerCase().endsWith(".pdf"));

        if (!isPdf) {
            return extracted.trim().length() < properties.ocr().minExtractedChars();
        }

        return extracted.trim().length() < properties.ocr().minExtractedChars();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private double estimateParsedTextConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        if (text.length() > 1500) {
            return 0.98;
        }
        if (text.length() > 500) {
            return 0.95;
        }
        if (text.length() > 150) {
            return 0.85;
        }
        return 0.60;
    }

    private double estimateOcrConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        if (text.length() > 1200) {
            return 0.93;
        }
        if (text.length() > 400) {
            return 0.88;
        }
        return 0.75;
    }
}
