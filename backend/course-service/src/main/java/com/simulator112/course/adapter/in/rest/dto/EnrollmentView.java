package com.simulator112.course.adapter.in.rest.dto;

import java.util.UUID;

public record EnrollmentView(UUID id, UUID courseId, UUID studentId, UUID groupId) {
}
