package com.simulator112.course.domain.enrollment;

import java.util.UUID;

public record Enrollment(
        UUID id,
        UUID courseId,
        UUID groupId) {
}
