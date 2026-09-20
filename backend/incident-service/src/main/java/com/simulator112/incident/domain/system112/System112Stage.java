package com.simulator112.incident.domain.system112;

import com.simulator112.incident.domain.common.CallScenario;
import com.simulator112.incident.domain.common.IncidentStage;
import com.simulator112.incident.domain.common.Person;

import java.util.List;
import java.util.UUID;

public record System112Stage(
        UUID id,
        String title,
        int position,
        String classifierCode,
        Person victim,
        String description,
        List<CallScenario> calls) implements IncidentStage {
    public System112Stage {
        calls = calls == null ? List.of() : List.copyOf(calls);
    }
}
