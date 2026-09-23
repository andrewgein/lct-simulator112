package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.GetStudyGroupUseCase;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.AssignmentExecutionMode;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.course.CourseTargetType;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.exception.EnrollmentNotFoundException;
import com.simulator112.course.domain.group.StudyGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnrollmentApplicationServiceTest {
    private final EnrollmentRepository repository = mock(EnrollmentRepository.class);
    private final GetCourseUseCase getCourse = mock(GetCourseUseCase.class);
    private final GetStudyGroupUseCase getGroup = mock(GetStudyGroupUseCase.class);
    private final EnrollmentApplicationService service = new EnrollmentApplicationService(repository, getCourse, getGroup);
    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final Assignment assignment = new Assignment(UUID.randomUUID(), "Задание", null,
            AssignmentDifficulty.NORMAL, AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID()));
    private final Course course = new Course(UUID.randomUUID(), "Курс", null, CourseTargetType.SYSTEM_112, teacherId,
            List.of(new CourseMaterial(UUID.randomUUID(), "Лекция", "materials/test/lecture.md", "lecture.md",
                    "text/markdown", 10L)), List.of(assignment));

    @Test
    void assignsCourseToEveryGroupStudent() {
        UUID otherStudentId = UUID.randomUUID();
        when(getCourse.getCourse(course.id())).thenReturn(course);
        when(getGroup.getStudyGroup(groupId)).thenReturn(new StudyGroup(
                groupId, "Группа", teacherId, List.of(studentId, otherStudentId)));
        when(repository.findByCourseIdAndStudentId(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var enrollments = service.assignCourseToGroup(course.id(), groupId, teacherId);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments).allMatch(value -> value.courseId().equals(course.id()));
    }

    @Test
    void findsDistinctCoursesAssignedToOwnedGroup() {
        Enrollment first = new Enrollment(UUID.randomUUID(), course.id(), studentId, groupId);
        Enrollment second = new Enrollment(UUID.randomUUID(), course.id(), UUID.randomUUID(), groupId);
        when(getGroup.getStudyGroup(groupId)).thenReturn(new StudyGroup(
                groupId, "Группа", teacherId, List.of(studentId)));
        when(repository.findAllByGroupId(groupId)).thenReturn(List.of(first, second));
        when(getCourse.getCourse(course.id())).thenReturn(course);

        assertThat(service.findGroupCourses(groupId, teacherId)).containsExactly(course);
    }

    @Test
    void rejectsReadingCoursesOfForeignGroup() {
        when(getGroup.getStudyGroup(groupId)).thenReturn(new StudyGroup(
                groupId, "Группа", UUID.randomUUID(), List.of(studentId)));

        assertThatThrownBy(() -> service.findGroupCourses(groupId, teacherId))
                .isInstanceOf(CourseAccessDeniedException.class);
    }

    @Test
    void rejectsAssignmentByForeignTeacher() {
        when(getCourse.getCourse(course.id())).thenReturn(course);
        when(getGroup.getStudyGroup(groupId)).thenReturn(new StudyGroup(
                groupId, "Группа", UUID.randomUUID(), List.of(studentId)));

        assertThatThrownBy(() -> service.assignCourseToGroup(course.id(), groupId, teacherId))
                .isInstanceOf(CourseAccessDeniedException.class);
    }

    @Test
    void resolvesAssignmentOnlyForEnrolledStudent() {
        Enrollment enrollment = new Enrollment(UUID.randomUUID(), course.id(), studentId, groupId);
        when(repository.findAllByStudentId(studentId)).thenReturn(List.of(enrollment));
        when(getCourse.getCourse(course.id())).thenReturn(course);

        assertThat(service.getEnrollmentForAssignment(assignment.id(), studentId)).isEqualTo(enrollment);
        assertThatThrownBy(() -> service.getEnrollmentForAssignment(UUID.randomUUID(), studentId))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }
}
