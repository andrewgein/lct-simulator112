package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.course.AssignmentDifficulty;

import java.util.List;
import java.util.UUID;

public interface GetAnalyticsCatalogUseCase {
    AnalyticsCatalog getAnalyticsCatalog(UUID requesterId, String role);

    record AnalyticsCatalog(List<GroupEntry> groups) {
        public AnalyticsCatalog {
            groups = List.copyOf(groups);
        }
    }

    record GroupEntry(UUID id, String title, List<UUID> studentIds, List<CourseEntry> courses) {
        public GroupEntry {
            studentIds = List.copyOf(studentIds);
            courses = List.copyOf(courses);
        }
    }

    record CourseEntry(UUID id, String title, List<AssignmentEntry> assignments) {
        public CourseEntry {
            assignments = List.copyOf(assignments);
        }
    }

    record AssignmentEntry(UUID id, String title, AssignmentDifficulty difficulty, List<UUID> incidentIds) {
        public AssignmentEntry {
            incidentIds = List.copyOf(incidentIds);
        }
    }
}
