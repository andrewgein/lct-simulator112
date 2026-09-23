package com.simulator112.course.application.port.in;

import java.util.UUID;

public interface DeleteCourseUseCase {
    void deleteCourse(UUID courseId, UUID requesterId);
}
