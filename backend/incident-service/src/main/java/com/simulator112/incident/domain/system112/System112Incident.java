package com.simulator112.incident.domain.system112;

import com.simulator112.incident.domain.common.Address;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;

import java.util.List;
import java.util.UUID;

public record System112Incident(
        UUID id,
        String title,
        Address address,
        Difficulty difficulty,
        List<System112Stage> stages,
        System112Criteria criteria) implements Incident {
    public System112Incident {
        stages = stages == null ? List.of() : List.copyOf(stages);
    }

    @Override
    public IncidentTargetType targetType() {
        return IncidentTargetType.SYSTEM_112;
    }
}
