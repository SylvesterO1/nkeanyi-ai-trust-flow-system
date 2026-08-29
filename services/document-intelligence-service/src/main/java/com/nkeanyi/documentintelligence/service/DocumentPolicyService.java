package com.nkeanyi.documentintelligence.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class DocumentPolicyService {

    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    public PolicyAssessment assess(String filename, String contentType, String text, double confidence) {
        String searchable = ((filename == null ? "" : filename) + " " + (text == null ? "" : text)).toLowerCase(Locale.ROOT);
        List<String> findings = new ArrayList<>();

        boolean promptInjection = containsAny(searchable,
                "ignore previous instructions",
                "ignore all previous instructions",
                "reveal system prompt",
                "developer message",
                "exfiltrate",
                "disable safety",
                "jailbreak");
        if (promptInjection) {
            findings.add("Prompt-injection style instruction detected in untrusted document content.");
        }

        boolean dlp = SSN_PATTERN.matcher(text == null ? "" : text).find()
                || CARD_PATTERN.matcher(text == null ? "" : text).find()
                || containsAny(searchable, "api key", "secret key", "password=", "private key");
        if (dlp) {
            findings.add("Sensitive data indicator detected by DLP gate.");
        }

        boolean regulated = containsAny(searchable, "phi", "hipaa", "pci", "legal privilege", "export controlled");
        if (regulated) {
            findings.add("Regulated content indicator detected.");
        }

        if (confidence < 0.75) {
            findings.add("Extraction confidence below enterprise publishing threshold.");
        }

        String sensitivity = "INTERNAL";
        if (dlp) {
            sensitivity = "RESTRICTED";
        } else if (regulated || containsAny(searchable, "confidential", "restricted", "customer data")) {
            sensitivity = "CONFIDENTIAL";
        } else if (containsAny(searchable, "public")) {
            sensitivity = "PUBLIC";
        }

        boolean quarantine = promptInjection || dlp;
        boolean reviewRequired = quarantine || regulated || confidence < 0.75 || "CONFIDENTIAL".equals(sensitivity);
        String riskLevel = quarantine ? "HIGH" : reviewRequired ? "MEDIUM" : "LOW";
        String qualityGateStatus = quarantine ? "QUARANTINED" : reviewRequired ? "REVIEW_REQUIRED" : "PASSED";

        return new PolicyAssessment(sensitivity, reviewRequired, quarantine, riskLevel, qualityGateStatus, findings);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public record PolicyAssessment(
            String sensitivityLabel,
            boolean reviewRequired,
            boolean quarantine,
            String riskLevel,
            String qualityGateStatus,
            List<String> findings
    ) {}
}
