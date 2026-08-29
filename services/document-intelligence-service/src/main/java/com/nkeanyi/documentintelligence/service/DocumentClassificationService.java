package com.nkeanyi.documentintelligence.service;

import com.nkeanyi.documentintelligence.model.DocumentType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DocumentClassificationService {

    public DocumentType classify(String fileName, String extractedText) {
        String name = safe(fileName);
        String text = safe(extractedText);

        if (containsAny(name, text, "policy", "compliance policy", "risk policy", "aml policy")) {
            return DocumentType.POLICY;
        }

        if (looksLikeJob(name, text)) {
            return DocumentType.JOB;
        }

        if (containsAny(name, text, "passport", "driver license", "national id", "proof of address", "utility bill", "kyc")) {
            return DocumentType.KYC;
        }

        if (containsAny(name, text, "invoice", "amount due", "bill to", "invoice number")) {
            return DocumentType.INVOICE;
        }

        if (containsAny(name, text, "contract", "agreement", "terms and conditions", "effective date")) {
            return DocumentType.CONTRACT;
        }

        if (looksLikeAcademic(name, text)) {
            return DocumentType.ACADEMIC;
        }

        if (looksLikeResume(name, text)) {
            return DocumentType.RESUME;
        }

        if (containsAny(name, text, "certificate", "issued on", "certified", "completion certificate")) {
            return DocumentType.CERTIFICATE;
        }

        return DocumentType.GENERAL;
    }

    private boolean looksLikeJob(String fileName, String text) {
        boolean titleSignal = containsAny(fileName, text,
                "job",
                "position",
                "career",
                "employment",
                "vacancy");

        boolean applicationSignal = containsAny(fileName, text,
                "how to apply",
                "apply for positions",
                "search for positions",
                "positions that may interest you",
                "positions that you qualify for",
                "click to apply");

        boolean platformSignal = containsAny(fileName, text,
                "workday",
                "city of dallas",
                "job opening",
                "job posting");

        int score = 0;
        if (titleSignal) score++;
        if (applicationSignal) score++;
        if (platformSignal) score++;

        return score >= 2;
    }

    private boolean looksLikeResume(String fileName, String text) {
        boolean resumeTitle = containsAny(fileName, text, "curriculum vitae", "resume", "cv");
        boolean professionalSummary = text.contains("professional summary");
        boolean workExperience = text.contains("work experience");
        boolean skills = text.contains("technical skills") || text.contains("core skills");

        int score = 0;
        if (resumeTitle) score++;
        if (professionalSummary) score++;
        if (workExperience) score++;
        if (skills) score++;

        return score >= 2;
    }

    private boolean looksLikeAcademic(String fileName, String text) {
        return containsAny(fileName, text,
                "transcript",
                "university",
                "student id",
                "course code",
                "academic");
    }

    private boolean containsAny(String fileName, String text, String... needles) {
        for (String needle : needles) {
            String n = needle.toLowerCase(Locale.ROOT);
            if (fileName.contains(n) || text.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
