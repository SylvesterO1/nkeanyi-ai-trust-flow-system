package com.nkeanyi.knowledge.classification;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DocumentClassifier {

    public DocumentType classify(String source, String content) {
        String text = ((source == null ? "" : source) + " " + (content == null ? "" : content))
                .toLowerCase(Locale.ROOT);

        if (containsAny(text,
                "policy", "compliance", "procedure", "governance", "control requirement", "risk policy")) {
            return DocumentType.POLICY;
        }

        if (containsAny(text,
                "passport", "driver license", "proof of address", "utility bill", "kyc", "customer identification")) {
            return DocumentType.KYC;
        }

        if (containsAny(text,
                "agreement", "contract", "party", "term and condition", "effective date", "obligation")) {
            return DocumentType.CONTRACT;
        }

        if (containsAny(text,
                "invoice", "bill to", "amount due", "payment due", "subtotal", "tax")) {
            return DocumentType.INVOICE;
        }

        if (containsAny(text,
                "professional summary", "work experience", "education", "skills", "resume", "curriculum vitae")) {
            return DocumentType.RESUME;
        }

        if (containsAny(text,
                "university", "transcript", "student", "course", "faculty", "registrar", "degree")) {
            return DocumentType.ACADEMIC;
        }

        return DocumentType.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
