package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.AssignCourseToGroupUseCase;
import com.simulator112.course.application.port.in.FindGroupCoursesUseCase;
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
        FindStudentEnrollmentsUseCase, FindGroupCoursesUseCase {

    private final EnrollmentRepository enrollmentRepository;
    private final GetCourseUseCase getCourse;
    private final GetStudyGroupUseCase getStudyGroup;

    @Override
    @Transactional
    public Enrollment assignCourseToGroup(UUID courseId, UUID groupId, UUID requesterId) {
        Course course = getCourse.getCourse(courseId);
        StudyGroup group = getStudyGroup.getStudyGroup(groupId);
        if (!course.authorId().equals(requesterId) || !group.ownerId().equals(requesterId)) {
            throw new CourseAccessDeniedException("Назначить курс группе может только преподаватель, "
                    + "которому принадлежат и курс, и группа");
        }
        return enrollmentRepository.findByCourseIdAndGroupId(courseId, groupId)
                .orElseGet(() -> enrollmentRepository.save(new Enrollment(null, courseId, groupId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> findGroupCourses(UUID groupId, UUID requesterId) {
        StudyGroup group = getStudyGroup.getStudyGroup(groupId);
        if (!group.ownerId().equals(requesterId)) {
            throw new CourseAccessDeniedException("Просматривать назначенные курсы может только владелец группы");
        }
        return enrollmentRepository.findAllByGroupId(groupId).stream()
                .map(Enrollment::courseId)
                .distinct()
                .map(getCourse::getCourse)
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
        return enrollmentRepository.findAllByStudentId(studentId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Enrollment::courseId,
                        enrollment -> enrollment,
                        (first, duplicate) -> first,
                        java.util.LinkedHashMap::new))
                .values().stream().toList();
    }
}
