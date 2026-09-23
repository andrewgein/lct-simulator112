package com.simulator112.course.adapter.in.rest.dto;

import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.AssignmentExecutionMode;
import java.util.List;
import java.util.UUID;

public record AssignmentView(
        UUID id,
        int position,
        String title,
        String description,
        AssignmentDifficulty difficulty,
        AssignmentExecutionMode executionMode,
        List<UUID> incidentIds) {
}
