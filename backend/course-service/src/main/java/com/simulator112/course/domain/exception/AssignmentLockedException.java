package com.simulator112.course.domain.exception;

public class AssignmentLockedException extends RuntimeException {
    public AssignmentLockedException(String message) {
        super(message);
    }
}
