package com.simulator112.course.application.port.out;

import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.CourseTargetType;
import java.util.UUID;

public interface IncidentCatalogPort {
    IncidentDescriptor requireIncident(UUID incidentId);

    record IncidentDescriptor(UUID id, CourseTargetType targetType, AssignmentDifficulty difficulty) {}
}
