package com.simulator112.review_service.adapter.out.course;

import com.simulator112.review_service.application.port.out.CourseAssignmentsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class CourseServiceAssignmentsAdapter implements CourseAssignmentsPort {
    private final RestClient restClient;

    public CourseServiceAssignmentsAdapter(RestClient.Builder restClientBuilder,
                                           @Value("${review.course-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<CourseAssignments> findCourseForAssignment(UUID assignmentId, UUID userId) {
        try {
            var enrollment = restClient.get()
                    .uri("/api/v1/enrollments/for-assignment/{assignmentId}", assignmentId)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .body(EnrollmentSummary.class);
            if (enrollment == null) return Optional.empty();
            var course = restClient.get()
                    .uri("/api/v1/courses/{courseId}", enrollment.courseId())
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .body(CourseSummary.class);
            if (course == null) return Optional.empty();
            return Optional.of(new CourseAssignments(course.id(), course.title(),
                    course.assignments().stream().map(AssignmentSummary::id).toList()));
        } catch (RuntimeException exception) {
            log.error("Не удалось получить курс для задания {}", assignmentId, exception);
            return Optional.empty();
        }
    }

    private record EnrollmentSummary(UUID id, UUID courseId, UUID groupId) {
    }

    private record CourseSummary(UUID id, String title, List<AssignmentSummary> assignments) {
    }

    private record AssignmentSummary(UUID id) {
    }
}
