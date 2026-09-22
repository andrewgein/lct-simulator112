package com.simulator112.course.domain.exception;

import java.util.UUID;

public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException(UUID courseId, UUID studentId) {
        super("Слушатель " + studentId + " не записан на курс " + courseId);
    }
}
