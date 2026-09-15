package com.simulator112.incident.dto.request;

import com.simulator112.incident.model.enums.Difficulty;

public record UpdateLevelRequest(
    String title,
    Difficulty difficulty
) {
}
