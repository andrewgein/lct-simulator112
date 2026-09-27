package com.simulator112.course.adapter.out.persistence.repository;

import com.simulator112.course.adapter.out.persistence.entity.CertificateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCertificateRepository extends JpaRepository<CertificateJpaEntity, UUID> {
    Optional<CertificateJpaEntity> findByUserIdAndCourseId(UUID userId, UUID courseId);

    List<CertificateJpaEntity> findAllByUserIdOrderByIssuedAtDesc(UUID userId);
}
