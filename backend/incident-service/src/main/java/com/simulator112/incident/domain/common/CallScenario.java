package com.simulator112.incident.domain.common;

import java.util.List;
import java.util.UUID;

public record CallScenario(
        UUID id,
        int position,
        CallDirection direction,
        CounterpartyType counterparty,
        Person person,
        Gender gender,
        List<String> knownFacts,
        List<String> hiddenFacts,
        String aiContext,
        String emotionalState,
        String serviceCode) {
    public CallScenario(UUID id, int position, CallDirection direction, CounterpartyType counterparty,
                        Person person, Gender gender, List<String> knownFacts, List<String> hiddenFacts,
                        String aiContext, String emotionalState) {
        this(id, position, direction, counterparty, person, gender, knownFacts, hiddenFacts, aiContext, emotionalState, null);
    }

    public CallScenario {
        knownFacts = knownFacts == null ? List.of() : List.copyOf(knownFacts);
        hiddenFacts = hiddenFacts == null ? List.of() : List.copyOf(hiddenFacts);
    }
}
