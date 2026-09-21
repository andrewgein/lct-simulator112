package com.simulator112.incident.adapter.in.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record IncidentImportRequest(
        @NotEmpty List<@Valid IncidentRequest> incidents,
        boolean createLevels) {
}
