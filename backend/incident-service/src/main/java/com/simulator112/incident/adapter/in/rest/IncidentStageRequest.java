package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.CallScenario;
import com.simulator112.incident.domain.dds.DdsStageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record IncidentStageRequest(
        UUID id,
        @NotBlank String title,
        Integer position,
        List<String> classifierCodes,
        Integer victimCount,
        String description,
        DdsStageType type,
        Integer timeLimitSeconds,
        @NotNull List<@Valid CallScenario> calls) {
}
