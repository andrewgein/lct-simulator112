package com.simulator112.course.application.port.out;

import com.simulator112.course.domain.course.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {
    Course save(Course course);

    Optional<Course> findById(UUID courseId);

    List<Course> findAllByIds(List<UUID> courseIds);

    List<Course> findAllByAuthorId(UUID authorId);
}
