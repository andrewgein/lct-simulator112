package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.enrollment.Enrollment;

import java.util.UUID;

public interface GetEnrollmentUseCase {
    Enrollment getEnrollment(UUID courseId, UUID studentId);
}
