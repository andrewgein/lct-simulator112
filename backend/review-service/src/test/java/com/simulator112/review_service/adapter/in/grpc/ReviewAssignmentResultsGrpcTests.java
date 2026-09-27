package com.simulator112.review_service.adapter.in.grpc;

import com.simulator112.review.grpc.contract.GetAssignmentResultsRequest;
import com.simulator112.review.grpc.contract.GetAssignmentResultsResponse;
import com.simulator112.review_service.application.port.in.GetAssignmentResultsUseCase;
import com.simulator112.review_service.application.port.in.SubmitReviewUseCase;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReviewAssignmentResultsGrpcTests {
    @Test
    void returnsOneResultPerCompletedAssignmentWithOptionalGrade() {
        var query = mock(GetAssignmentResultsUseCase.class);
        var controller = new ReviewGrpcController(mock(SubmitReviewUseCase.class), query);
        var userId = UUID.randomUUID();
        var assignmentId = UUID.randomUUID();
        var missingId = UUID.randomUUID();
        when(query.get(userId, List.of(assignmentId, missingId)))
                .thenReturn(List.of(new GetAssignmentResultsUseCase.Result(assignmentId, 75, 100, 4)));
        var observer = new Capture<GetAssignmentResultsResponse>();

        controller.getAssignmentResults(GetAssignmentResultsRequest.newBuilder().setUserId(userId.toString())
                .addAssignmentIds(assignmentId.toString()).addAssignmentIds(missingId.toString()).build(), observer);

        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
        assertThat(observer.value.getResultsCount()).isEqualTo(1);
        assertThat(observer.value.getResults(0).getAssignmentId()).isEqualTo(assignmentId.toString());
        assertThat(observer.value.getResults(0).getGrade()).isEqualTo(4);
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
