package com.simulator112.course.application.port.in;

import java.util.UUID;

public interface UnassignCourseFromGroupUseCase {
    void unassignCourseFromGroup(UUID courseId, UUID groupId, UUID requesterId);
}
