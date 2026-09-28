package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.Person;

import java.util.List;
import java.util.Map;

public record PreparedCardTemplate(
        List<String> classifierCodes,
        Person applicant,
        int victimCount,
        Map<String, String> additionalInfo,
        List<String> assignedServices) {
    public PreparedCardTemplate {
        if (victimCount < 0) throw new IllegalArgumentException("Количество пострадавших не может быть отрицательным");
        classifierCodes = classifierCodes == null ? List.of() : List.copyOf(classifierCodes);
        additionalInfo = additionalInfo == null ? Map.of() : Map.copyOf(additionalInfo);
        assignedServices = assignedServices == null ? List.of() : List.copyOf(assignedServices);
    }
}
