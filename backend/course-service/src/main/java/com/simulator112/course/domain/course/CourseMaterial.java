package com.simulator112.course.domain.course;

import java.util.UUID;

public record CourseMaterial(
        UUID id,
        String title,
        String contentMarkdown) {
}
