package com.simulator112.course.domain.group;

import java.util.List;
import java.util.UUID;

public record StudyGroup(
        UUID id,
        String title,
        UUID ownerId,
        List<UUID> studentIds) {
    public StudyGroup {
        studentIds = studentIds == null ? List.of() : List.copyOf(studentIds);
    }
}
