package com.simulator112.course.domain.exception;

import java.util.UUID;

public class StudyGroupNotFoundException extends RuntimeException {
    public StudyGroupNotFoundException(UUID groupId) {
        super("Учебная группа не найдена: " + groupId);
    }
}
