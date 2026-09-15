package com.simulator112.incident.dto.request.embeddable;

import com.simulator112.incident.model.enums.Gender;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DialupDetailsRequest(
    Gender gender,

    @NotEmpty(message = "Список известных звонящему фактов не может быть пустым")
    List<String> knownFacts,

    List<String> hiddenFacts,

    String aiContext,

    String emotionalState
) {
}
