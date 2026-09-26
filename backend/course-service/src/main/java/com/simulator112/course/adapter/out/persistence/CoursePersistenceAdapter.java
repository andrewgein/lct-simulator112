package com.simulator112.course.adapter.out.persistence;

import com.simulator112.course.adapter.out.persistence.repository.SpringDataCourseRepository;
import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.domain.course.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CoursePersistenceAdapter implements CourseRepository {
    private final SpringDataCourseRepository repository;
    private final CoursePersistenceMapper mapper;

    @Override
    @Transactional
    public Course save(Course course) {
        return mapper.toDomain(repository.save(mapper.toEntity(course)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Course> findById(UUID courseId) {
        return repository.findById(courseId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findAllByAuthorId(UUID authorId) {
        return repository.findAllByAuthorIdAndDeletedAtIsNull(authorId).stream().map(mapper::toDomain).toList();
    }
}
