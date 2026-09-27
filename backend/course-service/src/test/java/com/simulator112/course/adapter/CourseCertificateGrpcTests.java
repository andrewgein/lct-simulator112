package com.simulator112.course.adapter;

import com.simulator112.course.adapter.in.grpc.CourseGrpcController;
import com.simulator112.course.application.port.in.FindOwnedStudyGroupsUseCase;
import com.simulator112.course.application.port.in.GetCourseUseCase;
import com.simulator112.course.application.port.in.GetEnrollmentUseCase;
import com.simulator112.course.application.service.CertificateApplicationService;
import com.simulator112.course.domain.group.StudyGroup;
import com.simulator112.course.grpc.contract.ReviewResultAcknowledgement;
import com.simulator112.course.grpc.contract.ReviewResultNotification;
import com.simulator112.course.grpc.contract.TeacherStudentRequest;
import com.simulator112.course.grpc.contract.TeacherStudentResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CourseCertificateGrpcTests {
    private final CertificateApplicationService certificates = mock(CertificateApplicationService.class);
    private final FindOwnedStudyGroupsUseCase groups = mock(FindOwnedStudyGroupsUseCase.class);
    private final CourseGrpcController controller = new CourseGrpcController(
            mock(GetEnrollmentUseCase.class), mock(GetCourseUseCase.class), groups, certificates);

    @Test
    void triggersCompletionCheckForSavedReview() {
        UUID user = UUID.randomUUID();
        UUID assignment = UUID.randomUUID();
        var observer = new Capture<ReviewResultAcknowledgement>();
        controller.notifyReviewResult(ReviewResultNotification.newBuilder()
                .setUserId(user.toString()).setAssignmentId(assignment.toString()).build(), observer);
        verify(certificates).checkCompletion(user, assignment);
        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
    }

    @Test
    void checksMembershipInTeachersOwnedGroups() {
        UUID teacher = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        when(groups.findOwnedStudyGroups(teacher)).thenReturn(List.of(new StudyGroup(UUID.randomUUID(), "Группа", teacher,
                List.of(student))));
        var observer = new Capture<TeacherStudentResponse>();
        controller.isStudentOfTeacher(TeacherStudentRequest.newBuilder()
                .setTeacherId(teacher.toString()).setStudentId(student.toString()).build(), observer);
        assertThat(observer.error).isNull();
        assertThat(observer.value.getBelongsToTeacher()).isTrue();
    }

    static class Capture<T> implements StreamObserver<T> {
        T value;
        Throwable error;
        boolean completed;
        public void onNext(T value) { this.value = value; }
        public void onError(Throwable error) { this.error = error; }
        public void onCompleted() { completed = true; }
    }
}
