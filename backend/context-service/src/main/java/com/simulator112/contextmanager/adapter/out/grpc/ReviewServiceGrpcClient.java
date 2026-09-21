package com.simulator112.contextmanager.adapter.out.grpc;

import java.util.concurrent.TimeUnit;

import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.adapter.grpc.mapper.FullContextMapper;
import com.simulator112.review.grpc.contract.SendOnReviewRequest;
import com.simulator112.contextmanager.application.port.out.ReviewPort;
import org.springframework.stereotype.Component;

import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewServiceGrpcClient implements ReviewPort {

    private final ReviewServiceGrpc.ReviewServiceBlockingStub reviewServiceStub;

    @Override
    public void send(TrainingContext context) {
        reviewServiceStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .sendOnReview(SendOnReviewRequest.newBuilder().setContext(FullContextMapper.toProto(context)).build());
    }
}
