package com.simulator112.course.adapter.out.review;

import com.simulator112.course.application.port.out.AssignmentResultsPort;
import com.simulator112.review.grpc.contract.GetAssignmentResultsRequest;
import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ReviewAssignmentResultsGrpcAdapter implements AssignmentResultsPort {
    private final ReviewServiceGrpc.ReviewServiceBlockingStub stub;

    @Override
    public List<Result> get(UUID userId, List<UUID> assignmentIds) {
        var request = GetAssignmentResultsRequest.newBuilder().setUserId(userId.toString())
                .addAllAssignmentIds(assignmentIds.stream().map(UUID::toString).toList()).build();
        return stub.withDeadlineAfter(3, TimeUnit.SECONDS).getAssignmentResults(request).getResultsList().stream()
                .map(value -> new Result(UUID.fromString(value.getAssignmentId()), value.getScore(),
                        value.getMaxScore(), value.hasGrade() ? value.getGrade() : null)).toList();
    }

    @Configuration
    static class GrpcConfiguration {
        @Bean(destroyMethod = "shutdown")
        ManagedChannel reviewChannel(@Value("${review.grpc.host:localhost}") String host,
                                     @Value("${review.grpc.port:9096}") int port) {
            return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        }

        @Bean
        ReviewServiceGrpc.ReviewServiceBlockingStub reviewStub(@Qualifier("reviewChannel") ManagedChannel channel) {
            return ReviewServiceGrpc.newBlockingStub(channel);
        }
    }
}
