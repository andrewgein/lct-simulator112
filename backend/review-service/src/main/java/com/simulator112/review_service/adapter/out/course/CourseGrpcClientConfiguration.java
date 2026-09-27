package com.simulator112.review_service.adapter.out.course;

import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CourseGrpcClientConfiguration {
    @Bean(destroyMethod = "shutdown")
    ManagedChannel courseChannel(@Value("${review.course-service.grpc.host:localhost}") String host,
                                 @Value("${review.course-service.grpc.port:9095}") int port) {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    @Bean
    CourseServiceGrpc.CourseServiceBlockingStub courseStub(ManagedChannel courseChannel) {
        return CourseServiceGrpc.newBlockingStub(courseChannel);
    }
}
