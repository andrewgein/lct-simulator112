package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.Address;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record DdsIncident(
        UUID id,
        String title,
        Address address,
        Difficulty difficulty,
        List<DdsStage> stages,
        PreparedCardTemplate preparedCardTemplate,
        InitialAssignment initialAssignment,
        UUID initialStageId,
        List<DdsStageTransition> transitions) implements Incident {
    public DdsIncident {
        stages = stages == null ? List.of() : List.copyOf(stages);
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
    }

    @Override
    public IncidentTargetType targetType() {
        return IncidentTargetType.DDS;
    }

    public Optional<UUID> nextStage(UUID currentStageId, boolean successful) {
        return transitions.stream()
                .filter(transition -> transition.stageId().equals(currentStageId))
                .findFirst()
                .map(transition -> successful ? transition.successStageId() : transition.failureStageId());
    }
}
