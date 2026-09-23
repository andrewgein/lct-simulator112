package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.course.Course;

import java.util.List;
import java.util.UUID;

public interface FindGroupCoursesUseCase {
    List<Course> findGroupCourses(UUID groupId, UUID requesterId);
}
