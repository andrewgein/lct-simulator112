package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.adapter.in.rest.dto.CourseRequest;
import com.simulator112.course.adapter.in.rest.dto.CourseView;
import com.simulator112.course.application.port.in.CreateCourseUseCase;
import com.simulator112.course.application.port.in.FindAuthoredCoursesUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.UpdateCourseUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseRestController {
    private final CreateCourseUseCase createCourse;
    private final UpdateCourseUseCase updateCourse;
    private final GetCourseUseCase getCourse;
    private final FindAuthoredCoursesUseCase findAuthoredCourses;
    private final CourseRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseView create(@RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody CourseRequest request) {
        return mapper.toView(createCourse.createCourse(mapper.toDomain(null, request, userId)));
    }

    @PutMapping("/{courseId}")
    public CourseView update(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID courseId,
                             @Valid @RequestBody CourseRequest request) {
        return mapper.toView(updateCourse.updateCourse(courseId, mapper.toDomain(courseId, request, userId), userId));
    }

    @GetMapping("/{courseId}")
    public CourseView get(@PathVariable UUID courseId) {
        return mapper.toView(getCourse.getCourse(courseId));
    }

    @GetMapping
    public List<CourseView> findAuthored(@RequestHeader("X-User-Id") UUID userId) {
        return findAuthoredCourses.findAuthoredCourses(userId).stream().map(mapper::toView).toList();
    }
}
