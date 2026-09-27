package com.simulator112.course.application.port.out;

import com.simulator112.course.domain.model.Certificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateStore {
    Certificate save(Certificate certificate);

    Optional<Certificate> findById(UUID id);

    Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId);

    List<Certificate> findByUserId(UUID userId);
}
