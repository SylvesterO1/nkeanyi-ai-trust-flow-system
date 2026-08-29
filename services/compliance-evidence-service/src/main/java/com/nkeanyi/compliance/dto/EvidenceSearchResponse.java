package com.nkeanyi.compliance.dto;

import java.util.List;

public record EvidenceSearchResponse(
        int count,
        List<EvidenceResponse> evidence
) {
}
