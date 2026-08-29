package com.nkeanyi.documentintelligence.ocr;

import com.nkeanyi.documentintelligence.config.DocumentIntelligenceProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    private final DocumentIntelligenceProperties properties;

    public OcrService(DocumentIntelligenceProperties properties) {
        this.properties = properties;
    }

    public String extractWithOcr(byte[] content, String contentType, String fileName) {
        if (!properties.ocr().enabled()) {
            return "";
        }

        try {
            if (isPdf(contentType, fileName)) {
                return ocrPdf(content);
            }
            if (isImage(contentType, fileName)) {
                return ocrImageBytes(content, fileName);
            }
            return "";
        } catch (Exception e) {
            throw new RuntimeException("OCR extraction failed for file: " + fileName, e);
        }
    }

    private String ocrPdf(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<String> pageTexts = new ArrayList<>();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                String text = ocrBufferedImage(image, "pdf-page-" + i);
                if (!text.isBlank()) {
                    pageTexts.add(text);
                }
            }

            return String.join("\n\n", pageTexts).trim();
        }
    }

    private String ocrImageBytes(byte[] imageBytes, String name) throws Exception {
        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return "";
            }
            return ocrBufferedImage(image, name);
        }
    }

    private String ocrBufferedImage(BufferedImage image, String name) throws Exception {
        File tempImage = Files.createTempFile("ocr-", ".png").toFile();
        File tempOutputBase = Files.createTempFile("ocr-out-", "").toFile();

        try {
            ImageIO.write(image, "png", tempImage);

            ProcessBuilder pb = new ProcessBuilder(
                    properties.ocr().tesseractCommand(),
                    tempImage.getAbsolutePath(),
                    tempOutputBase.getAbsolutePath(),
                    "-l",
                    "eng"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String console = readAll(process.getInputStream());
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("Tesseract failed: " + console);
            }

            File txtFile = new File(tempOutputBase.getAbsolutePath() + ".txt");
            if (!txtFile.exists()) {
                return "";
            }

            return Files.readString(txtFile.toPath(), StandardCharsets.UTF_8).trim();
        } finally {
            tempImage.delete();
            new File(tempOutputBase.getAbsolutePath() + ".txt").delete();
            tempOutputBase.delete();
        }
    }

    private String readAll(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private boolean isPdf(String contentType, String fileName) {
        return "application/pdf".equalsIgnoreCase(contentType) ||
                (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }

    private boolean isImage(String contentType, String fileName) {
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp");
    }
}
