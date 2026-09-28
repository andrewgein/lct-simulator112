package com.simulator112.contextmanager.domain.system112;

import java.util.List;

public record System112StageDetails(List<String> classifierCodes, int victimCount) {
    public System112StageDetails {
        classifierCodes = List.copyOf(classifierCodes);
    }
}
