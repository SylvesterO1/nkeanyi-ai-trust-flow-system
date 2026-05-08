package com.nkeanyi.documentintelligence.extract;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PdfTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String contentType, String fileName) {
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            return true;
        }
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extract(byte[] content, String fileName) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            return normalize(text);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract PDF text from: " + fileName, e);
        }
    }

    public List<ExtractedPage> extractPages(byte[] content, String fileName) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<ExtractedPage> pages = new ArrayList<>();

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = normalize(stripper.getText(document));
                pages.add(new ExtractedPage(page, text, text.length()));
            }

            return pages;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract PDF pages from: " + fileName, e);
        }
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
}
