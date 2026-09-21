package com.simulator112.review_service.adapter.in.grpc;

import com.simulator112.review.grpc.contract.GetReviewRequest;
import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import com.simulator112.review.grpc.contract.ReviewStatusResponse;
import com.simulator112.review.grpc.contract.SendOnReviewRequest;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.application.port.in.SubmitReviewUseCase;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class ReviewGrpcController extends ReviewServiceGrpc.ReviewServiceImplBase {
    private final SubmitReviewUseCase submitReview;
    private final GetReviewUseCase getReview;

    @Override
    public void sendOnReview(SendOnReviewRequest request, StreamObserver<ReviewStatusResponse> observer) {
        try {
            respond(submitReview.submit(ReviewSubmissionGrpcMapper.toDomain(request.getContext())).status(), observer);
        } catch (Exception exception) {
            fail(exception, observer);
        }
    }

    @Override
    public void getReview(GetReviewRequest request, StreamObserver<ReviewStatusResponse> observer) {
        try {
            respond(getReview.getByContextId(UUID.fromString(request.getUuid())).status(), observer);
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
