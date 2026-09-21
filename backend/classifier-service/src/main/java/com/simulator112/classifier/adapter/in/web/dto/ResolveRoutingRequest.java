package com.simulator112.classifier.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ResolveRoutingRequest(
        @NotNull
        Map<@NotBlank String, @NotBlank String> facts
) {
}
