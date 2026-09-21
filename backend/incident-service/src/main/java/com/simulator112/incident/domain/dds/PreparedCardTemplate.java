package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.Person;

import java.util.Map;

public record PreparedCardTemplate(
        String classifierCode,
        Person applicant,
        Person victim,
        Map<String, String> additionalInfo) {
    public PreparedCardTemplate {
        additionalInfo = additionalInfo == null ? Map.of() : Map.copyOf(additionalInfo);
    }
}
