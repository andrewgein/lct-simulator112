package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.Address;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.dds.DdsStageTransition;
import com.simulator112.incident.domain.dds.InitialAssignment;
import com.simulator112.incident.domain.dds.PreparedCardTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record IncidentRequest(
        @NotBlank String title,
        @NotNull @Valid Address address,
        @NotNull Difficulty difficulty,
        @NotNull IncidentTargetType targetType,
        @NotNull List<@Valid IncidentStageRequest> stages,
        @NotNull List<String> requiredQuestions,
        @NotNull List<String> expectedActions,
        List<String> criticalMistakes,
        @Valid PreparedCardTemplate preparedCardTemplate,
        @Valid InitialAssignment initialAssignment,
        UUID initialStageId,
        List<@Valid DdsStageTransition> transitions) {
}
