package com.simulator112.incident.dto.view.embeddable;

import com.simulator112.incident.model.enums.Gender;

import java.util.List;

public record DialupDetailsView(
        Gender gender,
        List<String> knownFacts,
        List<String> hiddenFacts,
        String aiContext,
        String emotionalState
) {
}
