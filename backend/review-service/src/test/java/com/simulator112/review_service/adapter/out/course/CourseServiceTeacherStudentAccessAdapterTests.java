package com.simulator112.review_service.adapter.out.course;

import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import com.simulator112.course.grpc.contract.TeacherStudentRequest;
import com.simulator112.course.grpc.contract.TeacherStudentResponse;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseServiceTeacherStudentAccessAdapterTests {
    private final CourseServiceGrpc.CourseServiceBlockingStub stub = mock(CourseServiceGrpc.CourseServiceBlockingStub.class);
    private final CourseServiceTeacherStudentAccessAdapter adapter = new CourseServiceTeacherStudentAccessAdapter(stub);

    @Test
    void returnsMembershipFromCourseService() {
        when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);
        when(stub.isStudentOfTeacher(any(TeacherStudentRequest.class)))
                .thenReturn(TeacherStudentResponse.newBuilder().setBelongsToTeacher(true).build());

        assertThat(adapter.isStudentOfTeacher(UUID.randomUUID(), UUID.randomUUID())).isTrue();
    }

    @Test
    void failsClosedWhenCourseServiceUnavailable() {
        when(stub.withDeadlineAfter(anyLong(), any())).thenReturn(stub);
        when(stub.isStudentOfTeacher(any(TeacherStudentRequest.class)))
                .thenThrow(Status.UNAVAILABLE.asRuntimeException());

        assertThat(adapter.isStudentOfTeacher(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }
}
