package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.AssignCourseToGroupUseCase;
import com.simulator112.course.application.port.in.FindStudentEnrollmentsUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.GetEnrollmentUseCase;
import com.simulator112.course.application.port.in.GetStudyGroupUseCase;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.exception.EnrollmentNotFoundException;
import com.simulator112.course.domain.group.StudyGroup;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentApplicationService implements AssignCourseToGroupUseCase, GetEnrollmentUseCase,
        FindStudentEnrollmentsUseCase {

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
        if (group.studentIds().isEmpty()) throw new IllegalArgumentException("В группе нет слушателей");
        return group.studentIds().stream()
                .map(studentId -> enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                        .orElseGet(() -> enrollmentRepository.save(
                                new Enrollment(null, courseId, studentId, groupId))))
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
    public Enrollment getEnrollmentForAssignment(UUID assignmentId, UUID studentId) {
        return enrollmentRepository.findAllByStudentId(studentId).stream()
                .filter(enrollment -> getCourse.getCourse(enrollment.courseId()).assignment(assignmentId).isPresent())
                .findFirst()
                .orElseThrow(() -> new EnrollmentNotFoundException(assignmentId, studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findStudentEnrollments(UUID studentId) {
        return enrollmentRepository.findAllByStudentId(studentId);
    }
}
