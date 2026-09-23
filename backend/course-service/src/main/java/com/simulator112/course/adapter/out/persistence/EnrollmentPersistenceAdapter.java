package com.simulator112.course.adapter.out.persistence;

import com.simulator112.course.adapter.out.persistence.repository.SpringDataEnrollmentRepository;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.domain.enrollment.Enrollment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EnrollmentPersistenceAdapter implements EnrollmentRepository {
    private final SpringDataEnrollmentRepository repository;
    private final CoursePersistenceMapper mapper;

    @Override
    @Transactional
    public Enrollment save(Enrollment enrollment) {
        return mapper.toDomain(repository.save(mapper.toEntity(enrollment)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Enrollment> findByCourseIdAndStudentId(UUID courseId, UUID studentId) {
        return repository.findByCourseIdAndStudentId(courseId, studentId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findAllByStudentId(UUID studentId) {
        return repository.findAllByStudentId(studentId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findAllByCourseId(UUID courseId) {
        return repository.findAllByCourseId(courseId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findAllByGroupId(UUID groupId) {
        return repository.findAllByGroupId(groupId).stream().map(mapper::toDomain).toList();
    }
}
