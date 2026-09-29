package com.simulator112.course.adapter.in.grpc;

import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.FindAllStudyGroupsUseCase;
import com.simulator112.course.application.service.CertificateApplicationService;
import com.simulator112.course.grpc.contract.ReviewResultNotification;
import com.simulator112.course.grpc.contract.ReviewResultAcknowledgement;
import com.simulator112.course.grpc.contract.TeacherStudentRequest;
import com.simulator112.course.grpc.contract.TeacherStudentResponse;
import com.simulator112.course.application.port.in.GetEnrollmentUseCase;
import com.simulator112.course.grpc.contract.AssignmentForUserResponse;
import com.simulator112.course.grpc.contract.AssignmentDifficulty;
import com.simulator112.course.grpc.contract.AssignmentExecutionMode;
import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import com.simulator112.course.grpc.contract.CourseTargetType;
import com.simulator112.course.grpc.contract.GetAssignmentForUserRequest;
import com.simulator112.course.domain.exception.EnrollmentNotFoundException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CourseGrpcController extends CourseServiceGrpc.CourseServiceImplBase {
    private final GetEnrollmentUseCase enrollments;
    private final GetCourseUseCase courses;
    private final FindAllStudyGroupsUseCase groups;
    private final CertificateApplicationService certificates;

    @Override
    public void notifyReviewResult(ReviewResultNotification request,
                                   StreamObserver<ReviewResultAcknowledgement> observer) {
        try {
            certificates.checkCompletion(UUID.fromString(request.getUserId()), UUID.fromString(request.getAssignmentId()));
            observer.onNext(ReviewResultAcknowledgement.getDefaultInstance());
            observer.onCompleted();
        } catch (IllegalArgumentException exception) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException());
        } catch (Exception exception) {
            observer.onError(Status.INTERNAL.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        }
    }

    @Override
    public void isStudentOfTeacher(TeacherStudentRequest request, StreamObserver<TeacherStudentResponse> observer) {
        try {
            UUID studentId = UUID.fromString(request.getStudentId());
            boolean belongs = groups.findAllStudyGroups().stream()
                    .anyMatch(group -> group.studentIds().contains(studentId));
            observer.onNext(TeacherStudentResponse.newBuilder().setBelongsToTeacher(belongs).build());
            observer.onCompleted();
        } catch (IllegalArgumentException exception) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException());
        } catch (Exception exception) {
            observer.onError(Status.INTERNAL.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        }
    }

    @Override
    public void getAssignmentForUser(GetAssignmentForUserRequest request,
                                     StreamObserver<AssignmentForUserResponse> observer) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            UUID assignmentId = UUID.fromString(request.getAssignmentId());
            var enrollment = enrollments.getEnrollmentForAssignment(assignmentId, userId);
            var course = courses.getCourse(enrollment.courseId());
            var assignment = course.assignment(assignmentId).orElseThrow();
            int position = course.assignments().indexOf(assignment);
            var assignmentProto = com.simulator112.course.grpc.contract.Assignment.newBuilder()
                    .setId(assignment.id().toString())
                    .setPosition(position)
                    .setTitle(assignment.title())
                    .setDescription(assignment.description() == null ? "" : assignment.description())
                    .setDifficulty(AssignmentDifficulty.valueOf("ASSIGNMENT_DIFFICULTY_" + assignment.difficulty().name()))
                    .setExecutionMode(AssignmentExecutionMode.valueOf(
                            "ASSIGNMENT_EXECUTION_MODE_" + assignment.executionMode().name()))
                    .addAllIncidentIds(assignment.incidentIds().stream().map(UUID::toString).toList());
            if (assignment.threshold3() != null) {
                assignmentProto.setThreshold3(assignment.threshold3())
                        .setThreshold4(assignment.threshold4())
                        .setThreshold5(assignment.threshold5());
            }
            observer.onNext(AssignmentForUserResponse.newBuilder()
                    .setCourseTargetType(CourseTargetType.valueOf(
                            "COURSE_TARGET_TYPE_" + course.targetType().name()))
                    .setAssignment(assignmentProto.build()).build());
            observer.onCompleted();
        } catch (EnrollmentNotFoundException exception) {
            observer.onError(Status.PERMISSION_DENIED.withDescription("Курс не назначен пользователю")
                    .withCause(exception).asRuntimeException());
        } catch (IllegalArgumentException exception) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage())
                    .withCause(exception).asRuntimeException());
        } catch (Exception exception) {
            observer.onError(Status.INTERNAL.withDescription(exception.getMessage())
                    .withCause(exception).asRuntimeException());
        }
    }
}
