package com.simulator112.course.application.port.out;

import com.simulator112.course.domain.enrollment.Enrollment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository {
    Enrollment save(Enrollment enrollment);

    void delete(Enrollment enrollment);

    void deleteAllByCourseId(UUID courseId);

    Optional<Enrollment> findByCourseIdAndGroupId(UUID courseId, UUID groupId);

    Optional<Enrollment> findByCourseIdAndStudentId(UUID courseId, UUID studentId);

    List<Enrollment> findAllByStudentId(UUID studentId);

    List<Enrollment> findAllByCourseId(UUID courseId);

    List<Enrollment> findAllByGroupId(UUID groupId);

    List<Enrollment> findAllByGroupIds(List<UUID> groupIds);
}
