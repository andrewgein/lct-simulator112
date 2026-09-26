package com.simulator112.course;

import com.simulator112.course.application.port.in.DeleteCourseUseCase;
import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.application.port.out.StudyGroupRepository;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.AssignmentExecutionMode;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseTargetType;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.group.StudyGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
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

    @Autowired
    private DeleteCourseUseCase deleteCourse;

    @MockitoBean
    private com.simulator112.course.application.port.out.IncidentCatalogPort incidentCatalog;

    @Test
    void persistsCourseWithOrderedMaterialsAndAssignments() {
        UUID authorId = UUID.randomUUID();
        Course saved = courseRepository.save(new Course(null, "Работа оператора 112", "Базовый курс", CourseTargetType.SYSTEM_112, authorId,
                List.of(new CourseMaterial(null, "Введение", "materials/test/introduction.md", "introduction.md", "text/markdown", 12L),
                        new CourseMaterial(null, "Регламент", "materials/test/rules.md", "rules.md", "text/markdown", 12L)),
                List.of(new Assignment(null, "Первое задание", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())),
                        new Assignment(null, "Второе задание", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())))));

        Course loaded = courseRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.targetType()).isEqualTo(CourseTargetType.SYSTEM_112);
        assertThat(loaded.materials()).extracting(CourseMaterial::title).containsExactly("Введение", "Регламент");
        assertThat(loaded.assignments()).allMatch(assignment -> assignment.difficulty() == AssignmentDifficulty.NORMAL
                && assignment.executionMode() == AssignmentExecutionMode.SEQUENTIAL);
        assertThat(loaded.assignments()).extracting(Assignment::title)
                .containsExactly("Первое задание", "Второе задание");
        assertThat(courseRepository.findAllByAuthorId(authorId)).hasSize(1);
    }

    @Test
    void excludesArchivedCourseFromAuthorListButKeepsItAvailableById() {
        UUID authorId = UUID.randomUUID();
        Course saved = courseRepository.save(new Course(null, "Архивный курс", null, CourseTargetType.SYSTEM_112,
                authorId, List.of(), List.of()));

        courseRepository.save(saved.archive(Instant.now()));

        assertThat(courseRepository.findAllByAuthorId(authorId)).isEmpty();
        assertThat(courseRepository.findById(saved.id())).get().extracting(Course::deletedAt).isNotNull();
    }

    @Test
    void deletesCourseEnrollmentsWhenCourseIsArchived() {
        UUID ownerId = UUID.randomUUID();
        StudyGroup group = studyGroupRepository.save(new StudyGroup(null, "Группа", ownerId,
                List.of(UUID.randomUUID())));
        Course course = courseRepository.save(new Course(null, "Удаляемый курс", null,
                CourseTargetType.SYSTEM_112, ownerId, List.of(), List.of()));
        enrollmentRepository.save(new Enrollment(null, course.id(), group.id()));

        deleteCourse.deleteCourse(course.id(), ownerId);

        assertThat(courseRepository.findById(course.id())).get().extracting(Course::deletedAt).isNotNull();
        assertThat(enrollmentRepository.findAllByCourseId(course.id())).isEmpty();
    }

    @Test
    void deletesStudyGroupWithItsCourseEnrollments() {
        UUID ownerId = UUID.randomUUID();
        StudyGroup group = studyGroupRepository.save(new StudyGroup(null, "Удаляемая группа", ownerId,
                List.of(UUID.randomUUID())));
        Course course = courseRepository.save(new Course(null, "Курс группы", null, CourseTargetType.SYSTEM_112,
                ownerId, List.of(), List.of()));
        enrollmentRepository.save(new Enrollment(null, course.id(), group.id()));

        studyGroupRepository.deleteById(group.id());

        assertThat(studyGroupRepository.findById(group.id())).isEmpty();
        assertThat(enrollmentRepository.findAllByGroupId(group.id())).isEmpty();
        assertThat(courseRepository.findById(course.id())).isPresent();
    }

    @Test
    void persistsStudyGroupAndEnrollment() {
        UUID ownerId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudyGroup group = studyGroupRepository.save(new StudyGroup(null, "Группа 1", ownerId, List.of(studentId)));
        Course course = courseRepository.save(new Course(null, "Курс", null, CourseTargetType.SYSTEM_112, ownerId,
                List.of(new CourseMaterial(null, "Введение", "materials/test/other-introduction.md", "introduction.md", "text/markdown", 12L)),
                List.of(new Assignment(null, "Задание", null, AssignmentDifficulty.NORMAL,
                        AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())))));

        Enrollment enrollment = enrollmentRepository.save(
                new Enrollment(null, course.id(), group.id()));

        assertThat(enrollmentRepository.findByCourseIdAndStudentId(course.id(), studentId))
                .get().extracting(Enrollment::id).isEqualTo(enrollment.id());
        assertThat(enrollmentRepository.findAllByStudentId(studentId)).hasSize(1);

        UUID newStudentId = UUID.randomUUID();
        studyGroupRepository.save(new StudyGroup(group.id(), group.title(), ownerId, List.of(newStudentId)));

        assertThat(enrollmentRepository.findAllByStudentId(studentId)).isEmpty();
        assertThat(enrollmentRepository.findByCourseIdAndStudentId(course.id(), newStudentId)).isPresent();
        assertThat(studyGroupRepository.findAllByOwnerId(ownerId)).hasSize(1);
    }
}
