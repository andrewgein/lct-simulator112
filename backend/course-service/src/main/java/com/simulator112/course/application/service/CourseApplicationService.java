package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.CreateCourseUseCase;
import com.simulator112.course.application.port.in.DeleteCourseUseCase;
import com.simulator112.course.application.port.in.FindAuthoredCoursesUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.UpdateCourseUseCase;
import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.DispatchServiceCatalogPort;
import com.simulator112.course.application.port.out.IncidentCatalogPort;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.course.CourseTargetType;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.exception.CourseNotFoundException;
import com.simulator112.course.adapter.out.storage.MaterialFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseApplicationService implements CreateCourseUseCase, UpdateCourseUseCase, GetCourseUseCase,
        FindAuthoredCoursesUseCase, DeleteCourseUseCase {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final IncidentCatalogPort incidentCatalog;
    private final DispatchServiceCatalogPort dispatchServices;
    private final MaterialFileStorage fileStorage;

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
        Course updated = new Course(courseId, course.title(), course.description(), course.targetType(), course.ddsService(),
                existing.authorId(), course.materials(), course.assignments(), existing.deletedAt());
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

    @Override
    @Transactional
    public void deleteCourse(UUID courseId, UUID requesterId) {
        Course course = getCourse(courseId);
        if (!course.authorId().equals(requesterId)) {
            throw new CourseAccessDeniedException("Удалить курс может только его автор");
        }
        courseRepository.save(course.archive(Instant.now()));
        enrollmentRepository.deleteAllByCourseId(courseId);
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
        if (course.targetType() == null) {
            throw new IllegalArgumentException("Профиль курса обязателен");
        }
        if (course.targetType() == CourseTargetType.DDS && (course.ddsService() == null || course.ddsService().isBlank())) {
            throw new IllegalArgumentException("Для курса ДДС выберите специализацию");
        }
        if (course.targetType() == CourseTargetType.DDS) {
            dispatchServices.requireService(course.ddsService());
        }
        if (course.targetType() == CourseTargetType.SYSTEM_112 && course.ddsService() != null) {
            throw new IllegalArgumentException("Специализация ДДС недоступна для курса Системы-112");
        }
        if (course.assignments().isEmpty()) {
            throw new IllegalArgumentException("Курс должен содержать хотя бы одно задание");
        }
        course.materials().forEach(material -> {
            validate(material);
            fileStorage.requireOwnedBy(material.fileObjectKey(), course.authorId());
        });
        course.assignments().forEach(assignment -> validate(assignment, course));
    }

    private void validate(CourseMaterial material) {
        if (material.title() == null || material.title().isBlank()) {
            throw new IllegalArgumentException("Название вводного материала обязательно");
        }
        if (!material.hasFile()) {
            throw new IllegalArgumentException("Для вводного материала «" + material.title() + "» не загружен файл");
        }
    }

    private void validate(Assignment assignment, Course course) {
        if (assignment.title() == null || assignment.title().isBlank()) {
            throw new IllegalArgumentException("Название задания обязательно");
        }
        if (assignment.difficulty() == null || assignment.executionMode() == null) {
            throw new IllegalArgumentException("Сложность и режим выполнения задания обязательны");
        }
        if (assignment.incidentIds().isEmpty()) {
            throw new IllegalArgumentException("Задание «" + assignment.title()
                    + "» должно содержать хотя бы одно происшествие");
        }
        if (new HashSet<>(assignment.incidentIds()).size() != assignment.incidentIds().size()) {
            throw new IllegalArgumentException("Происшествие не может повторяться внутри задания «"
                    + assignment.title() + "»");
        }
        assignment.incidentIds().forEach(incidentId -> {
            var incident = incidentCatalog.requireIncident(incidentId);
            if (incident.targetType() != course.targetType()) {
                throw new IllegalArgumentException("Инцидент «" + incidentId
                        + "» не соответствует профилю курса " + course.targetType());
            }
        });
    }
}
