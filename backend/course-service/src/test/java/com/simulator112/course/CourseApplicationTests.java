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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID()), 40, 60, 80),
                        new Assignment(null, "Второе задание", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())))));

        Course loaded = courseRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.targetType()).isEqualTo(CourseTargetType.SYSTEM_112);
        assertThat(loaded.materials()).extracting(CourseMaterial::title).containsExactly("Введение", "Регламент");
        assertThat(loaded.assignments()).allMatch(assignment -> assignment.difficulty() == AssignmentDifficulty.NORMAL
                && assignment.executionMode() == AssignmentExecutionMode.SEQUENTIAL);
        assertThat(loaded.assignments()).extracting(Assignment::title)
                .containsExactly("Первое задание", "Второе задание");
        assertThat(loaded.assignments().getFirst().threshold3()).isEqualTo(40);
        assertThat(loaded.assignments().getFirst().threshold4()).isEqualTo(60);
        assertThat(loaded.assignments().getFirst().threshold5()).isEqualTo(80);
        assertThat(loaded.assignments().get(1).threshold3()).isNull();
        assertThat(courseRepository.findAllByAuthorId(authorId)).hasSize(1);
    }

    @Test
    void replacesAndReordersAssignmentsWithoutPositionConflict() {
        UUID authorId = UUID.randomUUID();
        Course saved = courseRepository.save(new Course(null, "Курс", null, CourseTargetType.SYSTEM_112, authorId,
                List.of(),
                List.of(new Assignment(null, "Старое первое", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())),
                        new Assignment(null, "Старое второе", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())))));

        Assignment retained = saved.assignments().get(1);
        Course updated = courseRepository.save(new Course(saved.id(), saved.title(), saved.description(),
                saved.targetType(), saved.authorId(), List.of(),
                List.of(new Assignment(null, "Новое первое", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())),
                        retained)));

        assertThat(updated.assignments()).extracting(Assignment::title)
                .containsExactly("Новое первое", "Старое второе");
        assertThat(updated.assignments().get(1).id()).isEqualTo(retained.id());
        Course loaded = courseRepository.findById(saved.id()).orElseThrow();
        assertThat(loaded.assignments()).extracting(Assignment::title)
                .containsExactly("Новое первое", "Старое второе");
        assertThat(loaded.assignments().get(1).id()).isEqualTo(retained.id());
    }

    @Test
    void reordersExistingMaterialsAndAssignments() {
        UUID authorId = UUID.randomUUID();
        Course saved = courseRepository.save(new Course(null, "Курс", null, CourseTargetType.SYSTEM_112, authorId,
                List.of(new CourseMaterial(null, "Первый", "materials/" + UUID.randomUUID(), "first.md", "text/markdown", 1L),
                        new CourseMaterial(null, "Второй", "materials/" + UUID.randomUUID(), "second.md", "text/markdown", 1L)),
                List.of(new Assignment(null, "Первое", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())),
                        new Assignment(null, "Второе", null, AssignmentDifficulty.NORMAL,
                                AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())))));

        courseRepository.save(new Course(saved.id(), saved.title(), saved.description(), saved.targetType(), authorId,
                List.of(saved.materials().get(1), saved.materials().get(0)),
                List.of(saved.assignments().get(1), saved.assignments().get(0))));

        Course loaded = courseRepository.findById(saved.id()).orElseThrow();
        assertThat(loaded.materials()).extracting(CourseMaterial::id)
                .containsExactly(saved.materials().get(1).id(), saved.materials().get(0).id());
        assertThat(loaded.assignments()).extracting(Assignment::id)
                .containsExactly(saved.assignments().get(1).id(), saved.assignments().get(0).id());
    }

    @Test
    void failedReplacementLeavesExistingChildrenIntact() {
        UUID authorId = UUID.randomUUID();
        String occupiedKey = "materials/" + UUID.randomUUID();
        courseRepository.save(new Course(null, "Другой курс", null, CourseTargetType.SYSTEM_112, authorId,
                List.of(new CourseMaterial(null, "Файл", occupiedKey, "file.md", "text/markdown", 1L)), List.of()));
        Course saved = courseRepository.save(new Course(null, "Курс", null, CourseTargetType.SYSTEM_112, authorId,
                List.of(new CourseMaterial(null, "Исходный", "materials/" + UUID.randomUUID(), "old.md", "text/markdown", 1L)),
                List.of(new Assignment(null, "Исходное", null, AssignmentDifficulty.NORMAL,
                        AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())))));

        assertThatThrownBy(() -> courseRepository.save(new Course(saved.id(), "Обновлённый", null,
                CourseTargetType.SYSTEM_112, authorId,
                List.of(new CourseMaterial(null, "Конфликт", occupiedKey, "new.md", "text/markdown", 1L)),
                List.of(new Assignment(null, "Новое", null, AssignmentDifficulty.NORMAL,
                        AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID())))))).isInstanceOf(RuntimeException.class);

        Course loaded = courseRepository.findById(saved.id()).orElseThrow();
        assertThat(loaded.title()).isEqualTo("Курс");
        assertThat(loaded.materials()).extracting(CourseMaterial::id).containsExactly(saved.materials().get(0).id());
        assertThat(loaded.assignments()).extracting(Assignment::id).containsExactly(saved.assignments().get(0).id());
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
