package com.simulator112.contextmanager.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateContextDto(@NotNull UUID assignmentId) {
}
