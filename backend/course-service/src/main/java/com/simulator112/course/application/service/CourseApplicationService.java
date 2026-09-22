package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.CreateCourseUseCase;
import com.simulator112.course.application.port.in.FindAuthoredCoursesUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.UpdateCourseUseCase;
import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.IncidentCatalogPort;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.exception.CourseNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseApplicationService implements CreateCourseUseCase, UpdateCourseUseCase, GetCourseUseCase,
        FindAuthoredCoursesUseCase {

    private final CourseRepository courseRepository;
    private final IncidentCatalogPort incidentCatalog;

    @Override
    @Transactional
    public Course createCourse(Course course) {
        validate(course);
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course updateCourse(UUID courseId, Course course, UUID requesterId) {
        Course existing = getCourse(courseId);
        if (!existing.authorId().equals(requesterId)) {
            throw new CourseAccessDeniedException("Изменять курс может только его автор");
        }
        if (course.id() != null && !courseId.equals(course.id())) {
            throw new IllegalArgumentException("Идентификатор курса нельзя изменить");
        }
        Course updated = new Course(courseId, course.title(), course.description(), existing.authorId(),
                course.materials(), course.assignments());
        validate(updated);
        return courseRepository.save(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getCourse(UUID courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findAuthoredCourses(UUID authorId) {
        return courseRepository.findAllByAuthorId(authorId);
    }

    private void validate(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Курс обязателен");
        }
        if (course.authorId() == null) {
            throw new IllegalArgumentException("Автор курса обязателен");
        }
        if (course.title() == null || course.title().isBlank()) {
            throw new IllegalArgumentException("Название курса обязательно");
        }
        if (course.materials().isEmpty()) {
            throw new IllegalArgumentException("Курс должен содержать хотя бы один вводный материал");
        }
        if (course.assignments().isEmpty()) {
            throw new IllegalArgumentException("Курс должен содержать хотя бы одно задание");
        }
        course.materials().forEach(this::validate);
        course.assignments().forEach(this::validate);
    }

    private void validate(CourseMaterial material) {
        if (material.title() == null || material.title().isBlank()) {
            throw new IllegalArgumentException("Название вводного материала обязательно");
        }
        if (material.contentMarkdown() == null || material.contentMarkdown().isBlank()) {
            throw new IllegalArgumentException("Вводный материал «" + material.title() + "» пуст");
        }
    }

    private void validate(Assignment assignment) {
        if (assignment.title() == null || assignment.title().isBlank()) {
            throw new IllegalArgumentException("Название задания обязательно");
        }
        if (assignment.incidentIds().isEmpty()) {
            throw new IllegalArgumentException("Задание «" + assignment.title()
                    + "» должно содержать хотя бы одно происшествие");
        }
        if (new HashSet<>(assignment.incidentIds()).size() != assignment.incidentIds().size()) {
            throw new IllegalArgumentException("Происшествие не может повторяться внутри задания «"
                    + assignment.title() + "»");
        }
        assignment.incidentIds().forEach(incidentCatalog::requireIncident);
    }
}
