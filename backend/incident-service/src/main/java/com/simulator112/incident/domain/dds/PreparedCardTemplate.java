package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.Person;

import java.util.List;
import java.util.Map;

public record PreparedCardTemplate(
        List<String> classifierCodes,
        Person applicant,
        Person victim,
        Map<String, String> additionalInfo) {
    public PreparedCardTemplate {
        classifierCodes = classifierCodes == null ? List.of() : List.copyOf(classifierCodes);
        additionalInfo = additionalInfo == null ? Map.of() : Map.copyOf(additionalInfo);
    }
}
