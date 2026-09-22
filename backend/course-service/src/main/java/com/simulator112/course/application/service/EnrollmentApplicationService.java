package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.AssignCourseToGroupUseCase;
import com.simulator112.course.application.port.in.CompleteAssignmentUseCase;
import com.simulator112.course.application.port.in.CompleteMaterialsUseCase;
import com.simulator112.course.application.port.in.FindStudentEnrollmentsUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.GetEnrollmentUseCase;
import com.simulator112.course.application.port.in.GetStudyGroupUseCase;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.exception.AssignmentLockedException;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.exception.EnrollmentNotFoundException;
import com.simulator112.course.domain.group.StudyGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentApplicationService implements AssignCourseToGroupUseCase, GetEnrollmentUseCase,
        FindStudentEnrollmentsUseCase, CompleteMaterialsUseCase, CompleteAssignmentUseCase {

    private final EnrollmentRepository enrollmentRepository;
    private final GetCourseUseCase getCourse;
    private final GetStudyGroupUseCase getStudyGroup;

    @Override
    @Transactional
    public List<Enrollment> assignCourseToGroup(UUID courseId, UUID groupId, UUID requesterId) {
        Course course = getCourse.getCourse(courseId);
        StudyGroup group = getStudyGroup.getStudyGroup(groupId);
        if (!course.authorId().equals(requesterId) || !group.ownerId().equals(requesterId)) {
            throw new CourseAccessDeniedException("Назначить курс группе может только преподаватель, "
                    + "которому принадлежат и курс, и группа");
        }
        if (group.studentIds().isEmpty()) {
            throw new IllegalArgumentException("В группе нет слушателей");
        }
        return group.studentIds().stream()
                .map(studentId -> enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                        .orElseGet(() -> enrollmentRepository.save(new Enrollment(null, courseId, studentId, groupId,
                                null, List.of(), null))))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Enrollment getEnrollment(UUID courseId, UUID studentId) {
        return enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(courseId, studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findStudentEnrollments(UUID studentId) {
        return enrollmentRepository.findAllByStudentId(studentId);
    }

    @Override
    @Transactional
    public Enrollment completeMaterials(UUID courseId, UUID studentId) {
        Enrollment enrollment = getEnrollment(courseId, studentId);
        if (enrollment.materialsCompleted()) {
            return enrollment;
        }
        return enrollmentRepository.save(enrollment.withMaterialsCompletedAt(Instant.now()));
    }

    @Override
    @Transactional
    public Enrollment completeAssignment(UUID courseId, UUID assignmentId, UUID studentId) {
        Enrollment enrollment = getEnrollment(courseId, studentId);
        Course course = getCourse.getCourse(courseId);
        if (!enrollment.materialsCompleted()) {
            throw new AssignmentLockedException("Сначала нужно ознакомиться с вводными материалами курса");
        }
        course.assignment(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Задание не найдено в курсе: " + assignmentId));
        Assignment next = course.nextAssignment(enrollment.completedAssignmentIds())
                .orElseThrow(() -> new AssignmentLockedException("Все задания курса уже выполнены"));
        if (!next.id().equals(assignmentId)) {
            throw new AssignmentLockedException("Задания проходятся последовательно, текущее задание: «"
                    + next.title() + "»");
        }
        boolean lastAssignment = course.assignments().size() == enrollment.completedAssignmentIds().size() + 1;
        return enrollmentRepository.save(
                enrollment.withCompletedAssignment(assignmentId, lastAssignment, Instant.now()));
    }
}
