package com.simulator112.contextmanager.application.port.out;

import com.simulator112.contextmanager.domain.common.AssignmentScenario;
import java.util.UUID;

public interface CourseAssignmentPort {
    AssignmentScenario getAssignmentForUser(UUID assignmentId, UUID userId);
}
