package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.enrollment.Enrollment;

import java.util.List;
import java.util.UUID;

public interface FindStudentEnrollmentsUseCase {
    List<Enrollment> findStudentEnrollments(UUID studentId);
}
