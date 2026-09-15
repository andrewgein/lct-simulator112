package com.simulator112.incident.dto.request;

import com.simulator112.incident.model.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLevelRequest(
    @NotBlank
    String title,

    @NotNull
    Difficulty difficulty
) {

}
