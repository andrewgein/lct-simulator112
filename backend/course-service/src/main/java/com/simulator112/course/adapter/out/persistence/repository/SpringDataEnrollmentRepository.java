package com.simulator112.course.adapter.out.persistence.repository;

import com.simulator112.course.adapter.out.persistence.entity.EnrollmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, UUID> {
    Optional<EnrollmentJpaEntity> findByCourseIdAndGroupId(UUID courseId, UUID groupId);

    List<EnrollmentJpaEntity> findAllByCourseId(UUID courseId);

    void deleteAllByCourseId(UUID courseId);

    List<EnrollmentJpaEntity> findAllByGroupId(UUID groupId);

    @Query(value = """
            SELECT enrollment.*
            FROM course_enrollments enrollment
            JOIN study_group_students student ON student.group_id = enrollment.group_id
            WHERE student.student_id = :studentId
            """, nativeQuery = true)
    List<EnrollmentJpaEntity> findAllByStudentId(@Param("studentId") UUID studentId);

    @Query(value = """
            SELECT enrollment.*
            FROM course_enrollments enrollment
            JOIN study_group_students student ON student.group_id = enrollment.group_id
            WHERE enrollment.course_id = :courseId AND student.student_id = :studentId
            LIMIT 1
            """, nativeQuery = true)
    Optional<EnrollmentJpaEntity> findByCourseIdAndStudentId(
            @Param("courseId") UUID courseId, @Param("studentId") UUID studentId);
}
