package com.simulator112.course.domain.exception;

import java.util.UUID;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(UUID courseId) {
        super("Курс не найден: " + courseId);
    }
}
