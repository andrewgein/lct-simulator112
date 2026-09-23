package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.adapter.in.rest.dto.EnrollmentView;
import com.simulator112.course.application.port.in.FindStudentEnrollmentsUseCase;
import com.simulator112.course.application.port.in.GetEnrollmentUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentRestController {
    private final FindStudentEnrollmentsUseCase findStudentEnrollments;
    private final GetEnrollmentUseCase getEnrollment;
    private final CourseRestMapper mapper;

    @GetMapping
    public List<EnrollmentView> findMine(@RequestHeader("X-User-Id") UUID userId) {
        return findStudentEnrollments.findStudentEnrollments(userId).stream().map(mapper::toView).toList();
    }

    @GetMapping("/{courseId}")
    public EnrollmentView get(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId) {
        return mapper.toView(getEnrollment.getEnrollment(courseId, userId));
    }
}
