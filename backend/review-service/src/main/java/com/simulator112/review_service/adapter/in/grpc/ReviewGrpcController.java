package com.simulator112.review_service.adapter.in.grpc;

import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import com.simulator112.review.grpc.contract.GetAssignmentResultsRequest;
import com.simulator112.review.grpc.contract.GetAssignmentResultsResponse;
import com.simulator112.review.grpc.contract.AssignmentResult;
import com.simulator112.review_service.application.port.in.GetAssignmentResultsUseCase;
import com.simulator112.review.grpc.contract.ReviewStatusResponse;
import com.simulator112.review.grpc.contract.SendOnReviewRequest;
import com.simulator112.review_service.application.port.in.SubmitReviewUseCase;
import io.grpc.Status;
import java.util.UUID;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;


@GrpcService
@RequiredArgsConstructor
@Slf4j
public class ReviewGrpcController extends ReviewServiceGrpc.ReviewServiceImplBase {
    private final SubmitReviewUseCase submitReview;
    private final GetAssignmentResultsUseCase assignmentResults;

    @Override
    public void sendOnReview(SendOnReviewRequest request, StreamObserver<ReviewStatusResponse> observer) {
        try {
            respond(submitReview.submit(ReviewSubmissionGrpcMapper.toDomain(request.getContext())).status(), observer);
        } catch (Exception exception) {
            fail(exception, observer);
        }
    }

    @Override
    public void getAssignmentResults(GetAssignmentResultsRequest request,
                                     StreamObserver<GetAssignmentResultsResponse> observer) {
        try {
            var response = GetAssignmentResultsResponse.newBuilder();
            for (var result : assignmentResults.get(UUID.fromString(request.getUserId()),
                    request.getAssignmentIdsList().stream().map(UUID::fromString).toList())) {
                var item = AssignmentResult.newBuilder().setAssignmentId(result.assignmentId().toString())
                        .setScore(result.score()).setMaxScore(result.maxScore());
                if (result.grade() != null) item.setGrade(result.grade());
                response.addResults(item);
            }
            observer.onNext(response.build());
            observer.onCompleted();
        } catch (IllegalArgumentException exception) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException());
        } catch (Exception exception) {
            fail(exception, observer);
        }
    }

    private void respond(com.simulator112.review_service.domain.model.ReviewStatus status,
                         StreamObserver<ReviewStatusResponse> observer) {
        observer.onNext(ReviewStatusResponse.newBuilder().setStatus(
                com.simulator112.review.grpc.contract.ReviewStatus.valueOf(status.name())).build());
        observer.onCompleted();
    }

    private void fail(Exception exception, StreamObserver<?> observer) {
        log.error("Ошибка review-service", exception);
        observer.onError(Status.INTERNAL.withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
    }
}
