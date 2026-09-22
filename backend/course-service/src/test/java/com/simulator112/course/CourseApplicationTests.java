package com.simulator112.course;

import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.application.port.out.StudyGroupRepository;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.group.StudyGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CourseApplicationTests {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @MockitoBean
    private com.simulator112.course.application.port.out.IncidentCatalogPort incidentCatalog;

    @Test
    void persistsCourseWithOrderedMaterialsAndAssignments() {
        UUID authorId = UUID.randomUUID();
        Course saved = courseRepository.save(new Course(null, "Работа оператора 112", "Базовый курс", authorId,
                List.of(new CourseMaterial(null, "Введение", "# Введение"),
                        new CourseMaterial(null, "Регламент", "# Регламент")),
                List.of(new Assignment(null, "Первое задание", null, List.of(UUID.randomUUID())),
                        new Assignment(null, "Второе задание", null, List.of(UUID.randomUUID())))));

        Course loaded = courseRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.materials()).extracting(CourseMaterial::title).containsExactly("Введение", "Регламент");
        assertThat(loaded.assignments()).extracting(Assignment::title)
                .containsExactly("Первое задание", "Второе задание");
        assertThat(courseRepository.findAllByAuthorId(authorId)).hasSize(1);
    }

    @Test
    void persistsStudyGroupAndEnrollment() {
        UUID ownerId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudyGroup group = studyGroupRepository.save(new StudyGroup(null, "Группа 1", ownerId, List.of(studentId)));
        Course course = courseRepository.save(new Course(null, "Курс", null, ownerId,
                List.of(new CourseMaterial(null, "Введение", "# Введение")),
                List.of(new Assignment(null, "Задание", null, List.of(UUID.randomUUID())))));

        Enrollment enrollment = enrollmentRepository.save(
                new Enrollment(null, course.id(), studentId, group.id(), null, List.of(), null));

        assertThat(enrollmentRepository.findByCourseIdAndStudentId(course.id(), studentId))
                .get().extracting(Enrollment::id).isEqualTo(enrollment.id());
        assertThat(enrollmentRepository.findAllByStudentId(studentId)).hasSize(1);
        assertThat(studyGroupRepository.findAllByOwnerId(ownerId)).hasSize(1);
    }
}
