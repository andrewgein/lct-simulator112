package com.simulator112.course.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

public record StudyGroupView(
        UUID id,
        String title,
        UUID ownerId,
        List<UUID> studentIds) {
}
