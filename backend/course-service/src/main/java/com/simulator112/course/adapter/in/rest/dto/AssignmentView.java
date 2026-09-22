package com.simulator112.course.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

public record AssignmentView(
        UUID id,
        int position,
        String title,
        String description,
        List<UUID> incidentIds) {
}
