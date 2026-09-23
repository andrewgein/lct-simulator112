package com.simulator112.course.adapter.out.persistence.repository;

import com.simulator112.course.adapter.out.persistence.entity.CourseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataCourseRepository extends JpaRepository<CourseJpaEntity, UUID> {
    List<CourseJpaEntity> findAllByAuthorIdAndDeletedAtIsNull(UUID authorId);
}
