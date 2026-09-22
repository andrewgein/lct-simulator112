package com.simulator112.course.domain.exception;

public class CourseAccessDeniedException extends RuntimeException {
    public CourseAccessDeniedException(String message) {
        super(message);
    }
}
