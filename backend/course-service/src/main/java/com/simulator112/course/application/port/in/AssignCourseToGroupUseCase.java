package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.enrollment.Enrollment;
import java.util.UUID;

public interface AssignCourseToGroupUseCase {
    Enrollment assignCourseToGroup(UUID courseId, UUID groupId, UUID requesterId);
}
