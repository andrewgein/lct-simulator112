package com.simulator112.contextmanager.domain.system112;

import java.util.List;
import java.util.Map;

public record System112StageDetails(List<String> classifierCodes, int victimCount, Map<String, String> expectedRoutingFacts) {
    public System112StageDetails(List<String> classifierCodes, int victimCount) {
        this(classifierCodes, victimCount, Map.of());
    }

    public System112StageDetails {
        classifierCodes = List.copyOf(classifierCodes);
        expectedRoutingFacts = Map.copyOf(expectedRoutingFacts);
    }
}
