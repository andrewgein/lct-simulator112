package com.simulator112.incident.dto.request.classifier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ResolveRoutingRequest(
        @NotNull
        Map<@NotBlank String, @NotBlank String> facts
) {
}
