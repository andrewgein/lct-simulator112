package com.simulator112.review_service.grpc.server;

import com.simulator112.review.grpc.contract.ReviewStatus;
import com.simulator112.review_service.service.ReviewService;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import com.simulator112.review.grpc.contract.SendOnReviewRequest;
import com.simulator112.review.grpc.contract.ReviewStatusResponse;
import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import com.simulator112.review_service.model.entity.Review;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GrpcService
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceGrpcServer extends ReviewServiceGrpc.ReviewServiceImplBase {

    private final ReviewService reviewService;

    @Override
    public void sendOnReview(SendOnReviewRequest request, StreamObserver<ReviewStatusResponse> responseObserver) {
        try {
            Review review = reviewService.review(request.getContext());
            ReviewStatusResponse response = ReviewStatusResponse.newBuilder()
                    .setStatus(ReviewStatus.valueOf(review.getStatus().name()))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Не удалось обработать контекст: {}", request, e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription(e.getMessage())
                            .asRuntimeException());
        }
    }
}
