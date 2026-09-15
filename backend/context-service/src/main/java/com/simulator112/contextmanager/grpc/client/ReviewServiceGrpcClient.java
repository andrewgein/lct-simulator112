package com.simulator112.contextmanager.grpc.client;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.review.grpc.contract.SendOnReviewRequest;
import com.simulator112.review.grpc.contract.ReviewStatusResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import com.simulator112.context.grpc.contract.GetFullContextRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewServiceGrpcClient {

    private final ReviewServiceGrpc.ReviewServiceBlockingStub reviewServiceStub;

    public ReviewStatusResponse sendFullContext(SendOnReviewRequest context) {
        ReviewStatusResponse response = reviewServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .sendOnReview(context);

        return response;
    }
}
