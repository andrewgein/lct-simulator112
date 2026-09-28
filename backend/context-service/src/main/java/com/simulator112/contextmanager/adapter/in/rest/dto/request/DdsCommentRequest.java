package com.simulator112.contextmanager.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DdsCommentRequest(@NotNull UUID stageId, @NotBlank @Size(max = 4000) String comment) {
}
