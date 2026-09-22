package com.simulator112.course.application.service;

import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.IncidentCatalogPort;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseApplicationServiceTest {
    private final CourseRepository repository = mock(CourseRepository.class);
    private final IncidentCatalogPort incidents = mock(IncidentCatalogPort.class);
    private final CourseApplicationService service = new CourseApplicationService(repository, incidents);

    private final UUID authorId = UUID.randomUUID();

    @Test
    void requiresMaterials() {
        Course course = new Course(null, "Курс", null, authorId, List.of(),
                List.of(new Assignment(null, "Задание", null, List.of(UUID.randomUUID()))));

        assertThatThrownBy(() -> service.createCourse(course))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("вводный материал");
    }

    @Test
    void requiresIncidentsInAssignment() {
        Course course = new Course(null, "Курс", null, authorId,
                List.of(new CourseMaterial(null, "Лекция", "# Лекция")),
                List.of(new Assignment(null, "Задание", null, List.of())));

        assertThatThrownBy(() -> service.createCourse(course))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("хотя бы одно происшествие");
    }

    @Test
    void rejectsUpdateByForeignAuthor() {
        UUID courseId = UUID.randomUUID();
        Course existing = new Course(courseId, "Курс", null, authorId,
                List.of(new CourseMaterial(null, "Лекция", "# Лекция")),
                List.of(new Assignment(null, "Задание", null, List.of(UUID.randomUUID()))));
        when(repository.findById(courseId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.updateCourse(courseId, existing, UUID.randomUUID()))
                .isInstanceOf(CourseAccessDeniedException.class);
    }
}
