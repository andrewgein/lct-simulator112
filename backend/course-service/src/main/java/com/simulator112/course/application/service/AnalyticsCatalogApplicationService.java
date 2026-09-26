package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.GetAnalyticsCatalogUseCase;
import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.application.port.out.StudyGroupRepository;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.group.StudyGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsCatalogApplicationService implements GetAnalyticsCatalogUseCase {
    private final StudyGroupRepository groups;
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsCatalog getAnalyticsCatalog(UUID requesterId, String role) {
        if (!"ADMIN".equals(role) && !"SUPERVISOR".equals(role)) {
            throw new IllegalArgumentException("Аналитика доступна только администратору и преподавателю");
        }
        List<StudyGroup> availableGroups = "ADMIN".equals(role)
                ? groups.findAll()
                : groups.findAllByOwnerId(requesterId);
        List<UUID> groupIds = availableGroups.stream().map(StudyGroup::id).toList();
        List<Enrollment> availableEnrollments = enrollments.findAllByGroupIds(groupIds);
        Map<UUID, Course> coursesById = courses.findAllByIds(availableEnrollments.stream()
                        .map(Enrollment::courseId).distinct().toList()).stream()
                .collect(Collectors.toMap(Course::id, Function.identity()));
        Map<UUID, List<Course>> coursesByGroup = availableEnrollments.stream()
                .filter(enrollment -> coursesById.containsKey(enrollment.courseId()))
                .collect(Collectors.groupingBy(Enrollment::groupId,
                        Collectors.mapping(enrollment -> coursesById.get(enrollment.courseId()), Collectors.toList())));
        return new AnalyticsCatalog(availableGroups.stream()
                .map(group -> new GroupEntry(group.id(), group.title(), group.studentIds(),
                        coursesByGroup.getOrDefault(group.id(), List.of()).stream()
                                .map(this::courseEntry).toList()))
                .toList());
    }

    private CourseEntry courseEntry(Course course) {
        return new CourseEntry(course.id(), course.title(), course.assignments().stream()
                .map(this::assignmentEntry).toList());
    }

    private AssignmentEntry assignmentEntry(Assignment assignment) {
        return new AssignmentEntry(assignment.id(), assignment.title(), assignment.difficulty(),
                assignment.incidentIds());
    }
}
