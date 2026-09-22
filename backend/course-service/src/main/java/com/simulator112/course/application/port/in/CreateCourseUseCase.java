package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.course.Course;

public interface CreateCourseUseCase {
    Course createCourse(Course course);
}
