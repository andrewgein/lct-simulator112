package com.simulator112.incident.domain.dds;

public record InitialAssignment(
        String emergencyService,
        String classifierCode,
        String instructions) {
}
