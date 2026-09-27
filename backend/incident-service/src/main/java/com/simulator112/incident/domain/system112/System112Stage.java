package com.simulator112.incident.domain.system112;

import com.simulator112.incident.domain.common.CallScenario;
import com.simulator112.incident.domain.common.IncidentStage;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record System112Stage(
        UUID id,
        String title,
        int position,
        List<String> classifierCodes,
        Map<String, String> expectedRoutingFacts,
        int victimCount,
        String description,
        List<CallScenario> calls) implements IncidentStage {
    public System112Stage(UUID id, String title, int position, List<String> classifierCodes,
                          int victimCount, String description, List<CallScenario> calls) {
        this(id, title, position, classifierCodes, Map.of(), victimCount, description, calls);
    }

    public System112Stage {
        if (victimCount < 0) throw new IllegalArgumentException("Количество пострадавших не может быть отрицательным");
        classifierCodes = classifierCodes == null ? List.of() : List.copyOf(classifierCodes);
        expectedRoutingFacts = expectedRoutingFacts == null ? Map.of() : Map.copyOf(expectedRoutingFacts);
        calls = calls == null ? List.of() : List.copyOf(calls);
    }
}
