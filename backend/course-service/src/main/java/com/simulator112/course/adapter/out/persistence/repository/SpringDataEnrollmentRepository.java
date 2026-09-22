package com.simulator112.course.adapter.out.persistence.repository;

import com.simulator112.course.adapter.out.persistence.entity.EnrollmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, UUID> {
    Optional<EnrollmentJpaEntity> findByCourseIdAndStudentId(UUID courseId, UUID studentId);

    List<EnrollmentJpaEntity> findAllByStudentId(UUID studentId);

    List<EnrollmentJpaEntity> findAllByCourseId(UUID courseId);
}
