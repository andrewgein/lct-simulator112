package com.simulator112.review_service.application.port.out;

import java.util.List;
import java.util.UUID;

public interface AnalyticsCatalogPort {
    Catalog getCatalog(UUID requesterId, String role);

    record Catalog(List<GroupEntry> groups) {
        public Catalog {
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

    record AssignmentEntry(UUID id, String title, String difficulty, List<UUID> incidentIds) {
        public AssignmentEntry {
            incidentIds = List.copyOf(incidentIds);
        }
    }
}
