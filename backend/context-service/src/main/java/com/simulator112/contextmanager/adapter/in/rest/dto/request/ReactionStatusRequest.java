package com.simulator112.contextmanager.adapter.in.rest.dto.request;

import com.simulator112.contextmanager.domain.common.ReactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReactionStatusRequest(@NotBlank String serviceCode, @NotNull ReactionStatus status, String comment) {
}
