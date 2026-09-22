package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.adapter.in.rest.dto.EnrollmentView;
import com.simulator112.course.application.port.in.CompleteAssignmentUseCase;
import com.simulator112.course.application.port.in.CompleteMaterialsUseCase;
import com.simulator112.course.application.port.in.FindStudentEnrollmentsUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.GetEnrollmentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentRestController {
    private final FindStudentEnrollmentsUseCase findStudentEnrollments;
    private final GetEnrollmentUseCase getEnrollment;
    private final CompleteMaterialsUseCase completeMaterials;
    private final CompleteAssignmentUseCase completeAssignment;
    private final GetCourseUseCase getCourse;
    private final CourseRestMapper mapper;

    @GetMapping
    public List<EnrollmentView> findMine(@RequestHeader("X-User-Id") UUID userId) {
        return findStudentEnrollments.findStudentEnrollments(userId).stream()
                .map(enrollment -> mapper.toView(enrollment, getCourse.getCourse(enrollment.courseId())))
                .toList();
    }

    @GetMapping("/{courseId}")
    public EnrollmentView get(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId) {
        return mapper.toView(getEnrollment.getEnrollment(courseId, userId), getCourse.getCourse(courseId));
    }

    @PostMapping("/{courseId}/materials")
    public EnrollmentView completeMaterials(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId) {
        return mapper.toView(completeMaterials.completeMaterials(courseId, userId), getCourse.getCourse(courseId));
    }

    @PostMapping("/{courseId}/assignments/{assignmentId}/complete")
    public EnrollmentView completeAssignment(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId,
                                             @PathVariable UUID assignmentId) {
        return mapper.toView(completeAssignment.completeAssignment(courseId, assignmentId, userId),
                getCourse.getCourse(courseId));
    }
}
