package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.GetStudyGroupUseCase;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.enrollment.EnrollmentStatus;
import com.simulator112.course.domain.exception.AssignmentLockedException;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnrollmentApplicationServiceTest {
    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final GetCourseUseCase getCourse = mock(GetCourseUseCase.class);
    private final GetStudyGroupUseCase getStudyGroup = mock(GetStudyGroupUseCase.class);
    private final EnrollmentApplicationService service =
            new EnrollmentApplicationService(enrollmentRepository, getCourse, getStudyGroup);

    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final Assignment first = new Assignment(UUID.randomUUID(), "Первое", null, List.of(UUID.randomUUID()));
    private final Assignment second = new Assignment(UUID.randomUUID(), "Второе", null, List.of(UUID.randomUUID()));
    private final Course course = new Course(UUID.randomUUID(), "Курс", null, teacherId,
            List.of(new CourseMaterial(UUID.randomUUID(), "Лекция", "# Лекция")), List.of(first, second));

    @Test
    void assignsCourseToEveryGroupStudent() {
        UUID otherStudentId = UUID.randomUUID();
        when(getCourse.getCourse(course.id())).thenReturn(course);
        when(getStudyGroup.getStudyGroup(groupId)).thenReturn(new com.simulator112.course.domain.group.StudyGroup(
                groupId, "Группа", teacherId, List.of(studentId, otherStudentId)));
        when(enrollmentRepository.findByCourseIdAndStudentId(any(), any())).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var enrollments = service.assignCourseToGroup(course.id(), groupId, teacherId);

        assertThat(enrollments).hasSize(2);
        assertThat(enrollments).allMatch(enrollment -> enrollment.status() == EnrollmentStatus.MATERIALS);
    }

    @Test
    void rejectsAssignmentByForeignTeacher() {
        when(getCourse.getCourse(course.id())).thenReturn(course);
        when(getStudyGroup.getStudyGroup(groupId)).thenReturn(new com.simulator112.course.domain.group.StudyGroup(
                groupId, "Группа", UUID.randomUUID(), List.of(studentId)));

        assertThatThrownBy(() -> service.assignCourseToGroup(course.id(), groupId, teacherId))
                .isInstanceOf(CourseAccessDeniedException.class);
    }

    @Test
    void requiresMaterialsBeforeAssignments() {
        stubEnrollment(enrollment(null, List.of()));

        assertThatThrownBy(() -> service.completeAssignment(course.id(), first.id(), studentId))
                .isInstanceOf(AssignmentLockedException.class)
                .hasMessageContaining("вводными материалами");
    }

    @Test
    void requiresSequentialAssignments() {
        stubEnrollment(enrollment(java.time.Instant.now(), List.of()));

        assertThatThrownBy(() -> service.completeAssignment(course.id(), second.id(), studentId))
                .isInstanceOf(AssignmentLockedException.class)
                .hasMessageContaining("последовательно");
    }

    @Test
    void completesCourseAfterLastAssignment() {
        stubEnrollment(enrollment(java.time.Instant.now(), List.of(first.id())));
        when(enrollmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment completed = service.completeAssignment(course.id(), second.id(), studentId);

        assertThat(completed.completedAssignmentIds()).containsExactly(first.id(), second.id());
        assertThat(completed.status()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    private void stubEnrollment(Enrollment enrollment) {
        when(getCourse.getCourse(course.id())).thenReturn(course);
        when(enrollmentRepository.findByCourseIdAndStudentId(course.id(), studentId))
                .thenReturn(Optional.of(enrollment));
    }

    private Enrollment enrollment(java.time.Instant materialsCompletedAt, List<UUID> completedAssignmentIds) {
        return new Enrollment(UUID.randomUUID(), course.id(), studentId, groupId, materialsCompletedAt,
                completedAssignmentIds, null);
    }
}
