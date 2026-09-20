package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.EmergencyService;

public record InitialAssignment(
        EmergencyService emergencyService,
        String classifierCode,
        String instructions) {
}
