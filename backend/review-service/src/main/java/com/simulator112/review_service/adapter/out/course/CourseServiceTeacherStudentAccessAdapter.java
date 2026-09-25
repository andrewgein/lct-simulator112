package com.simulator112.review_service.adapter.out.course;

import com.simulator112.review_service.application.port.out.TeacherStudentAccessPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class CourseServiceTeacherStudentAccessAdapter implements TeacherStudentAccessPort {
    private final RestClient restClient;

    public CourseServiceTeacherStudentAccessAdapter(RestClient.Builder restClientBuilder,
                                                     @Value("${review.course-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public boolean isStudentOfTeacher(UUID teacherId, UUID studentId) {
        try {
            StudyGroupSummary[] groups = restClient.get()
                    .uri("/api/v1/study-groups")
                    .header("X-User-Id", teacherId.toString())
                    .retrieve()
                    .body(StudyGroupSummary[].class);
            if (groups == null) return false;
            return List.of(groups).stream().anyMatch(group -> group.studentIds().contains(studentId));
        } catch (RuntimeException exception) {
            log.error("Не удалось проверить принадлежность ученика {} преподавателю {}",
                    studentId, teacherId, exception);
            return false;
        }
    }

    private record StudyGroupSummary(UUID id, UUID ownerId, List<UUID> studentIds) {
        private StudyGroupSummary {
            studentIds = studentIds == null ? List.of() : studentIds;
        }
    }
}
