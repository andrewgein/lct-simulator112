package com.simulator112.course.adapter.out.persistence;

import com.simulator112.course.adapter.out.persistence.entity.CertificateJpaEntity;
import com.simulator112.course.adapter.out.persistence.repository.SpringDataCertificateRepository;
import com.simulator112.course.application.port.out.CertificateStore;
import com.simulator112.course.domain.model.Certificate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CertificatePersistenceAdapter implements CertificateStore {
    private final SpringDataCertificateRepository repository;

    private static CertificateJpaEntity toEntity(Certificate source) {
        CertificateJpaEntity target = new CertificateJpaEntity();
        target.setId(source.id());
        target.setUserId(source.userId());
        target.setCourseId(source.courseId());
        target.setCourseTitle(source.courseTitle());
        target.setPercent(source.percent());
        target.setType(source.type());
        target.setIssuedAt(source.issuedAt());
        return target;
    }

    private static Certificate toDomain(CertificateJpaEntity source) {
        return new Certificate(source.getId(), source.getUserId(), source.getCourseId(), source.getCourseTitle(),
                source.getPercent(), source.getType(), source.getIssuedAt());
    }

    @Override
    public Certificate save(Certificate certificate) {
        return toDomain(repository.save(toEntity(certificate)));
    }

    @Override
    public Optional<Certificate> findById(UUID id) {
        return repository.findById(id).map(CertificatePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Certificate> findByUserIdAndCourseId(UUID userId, UUID courseId) {
        return repository.findByUserIdAndCourseId(userId, courseId).map(CertificatePersistenceAdapter::toDomain);
    }

    @Override
    public List<Certificate> findByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(CertificatePersistenceAdapter::toDomain).toList();
    }
}
