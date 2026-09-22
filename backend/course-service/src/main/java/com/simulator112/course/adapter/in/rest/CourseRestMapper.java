package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.adapter.in.rest.dto.AssignmentRequest;
import com.simulator112.course.adapter.in.rest.dto.AssignmentView;
import com.simulator112.course.adapter.in.rest.dto.CourseMaterialView;
import com.simulator112.course.adapter.in.rest.dto.CourseRequest;
import com.simulator112.course.adapter.in.rest.dto.CourseView;
import com.simulator112.course.adapter.in.rest.dto.EnrollmentView;
import com.simulator112.course.adapter.in.rest.dto.StudyGroupRequest;
import com.simulator112.course.adapter.in.rest.dto.StudyGroupView;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseMaterial;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.group.StudyGroup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CourseRestMapper {

    public Course toDomain(UUID courseId, CourseRequest request, UUID authorId) {
        return new Course(courseId, request.title(), request.description(), authorId,
                request.materials().stream()
                        .map(material -> new CourseMaterial(material.id(), material.title(),
                                material.contentMarkdown()))
                        .toList(),
                request.assignments().stream().map(this::toDomain).toList());
    }

    public StudyGroup toDomain(UUID groupId, StudyGroupRequest request, UUID ownerId) {
        return new StudyGroup(groupId, request.title(), ownerId, request.studentIds());
    }

    public CourseView toView(Course course) {
        return new CourseView(course.id(), course.title(), course.description(), course.authorId(),
                materials(course), assignments(course));
    }

    public StudyGroupView toView(StudyGroup studyGroup) {
        return new StudyGroupView(studyGroup.id(), studyGroup.title(), studyGroup.ownerId(), studyGroup.studentIds());
    }

    public EnrollmentView toView(Enrollment enrollment, Course course) {
        AssignmentView current = enrollment.materialsCompleted()
                ? course.nextAssignment(enrollment.completedAssignmentIds())
                .map(assignment -> toView(assignment, course.assignments().indexOf(assignment)))
                .orElse(null)
                : null;
        return new EnrollmentView(enrollment.id(), enrollment.courseId(), enrollment.studentId(), enrollment.groupId(),
                enrollment.status(), enrollment.materialsCompletedAt(), enrollment.completedAssignmentIds(),
                current, enrollment.completedAt());
    }

    private Assignment toDomain(AssignmentRequest request) {
        return new Assignment(request.id(), request.title(), request.description(), request.incidentIds());
    }

    private List<CourseMaterialView> materials(Course course) {
        List<CourseMaterial> materials = course.materials();
        return java.util.stream.IntStream.range(0, materials.size())
                .mapToObj(position -> new CourseMaterialView(materials.get(position).id(), position,
                        materials.get(position).title(), materials.get(position).contentMarkdown()))
                .toList();
    }

    private List<AssignmentView> assignments(Course course) {
        List<Assignment> assignments = course.assignments();
        return java.util.stream.IntStream.range(0, assignments.size())
                .mapToObj(position -> toView(assignments.get(position), position))
                .toList();
    }

    private AssignmentView toView(Assignment assignment, int position) {
        return new AssignmentView(assignment.id(), position, assignment.title(), assignment.description(),
                assignment.incidentIds());
    }
}
