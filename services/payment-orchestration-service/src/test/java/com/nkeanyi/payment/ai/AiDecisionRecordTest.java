package com.nkeanyi.payment.ai;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiDecisionRecordTest {

    @Test
    void shouldCreateImmutableDecisionRecord() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "internal-ai");

        AiDecisionRecord record = new AiDecisionRecord(
                "fraud-review",
                "gpt-risk",
                "2026-04",
                "input-ref-1",
                "output-ref-1",
                0.91,
                false,
                "APPROVED",
                Instant.parse("2026-04-16T22:00:00Z"),
                metadata
        );

        metadata.put("provider", "changed-after-construction");

        assertThat(record.metadata()).containsEntry("provider", "internal-ai");
        assertThat(record.hasConfidenceScore()).isTrue();
        assertThat(record.isApproved()).isTrue();
        assertThatThrownBy(() -> record.metadata().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectInvalidConfidenceScore() {
        assertThatThrownBy(() -> new AiDecisionRecord(
                "fraud-review",
                "gpt-risk",
                "2026-04",
                "input-ref-1",
                "output-ref-1",
                1.5,
                false,
                "APPROVED",
                Instant.parse("2026-04-16T22:00:00Z"),
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("confidenceScore must be between 0.0 and 1.0");
    }

    @Test
    void shouldRejectBlankRequiredFields() {
        assertThatThrownBy(() -> new AiDecisionRecord(
                " ",
                "gpt-risk",
                "2026-04",
                "input-ref-1",
                "output-ref-1",
                0.5,
                false,
                "APPROVED",
                Instant.parse("2026-04-16T22:00:00Z"),
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("useCase is required");
    }
}
