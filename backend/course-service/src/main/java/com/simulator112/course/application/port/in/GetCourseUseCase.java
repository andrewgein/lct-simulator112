package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.course.Course;

import java.util.UUID;

public interface GetCourseUseCase {
    Course getCourse(UUID courseId);
}
