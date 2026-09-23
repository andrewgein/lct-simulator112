package com.simulator112.contextmanager.adapter.out.grpc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import com.simulator112.incident.grpc.contract.IncidentGrpcServiceGrpc;

@Configuration
public class GrpcClientStubConfig {

    @Bean
    IncidentGrpcServiceGrpc.IncidentGrpcServiceBlockingStub incidentServiceStub(GrpcChannelFactory channels) {
        return IncidentGrpcServiceGrpc.newBlockingStub(channels.createChannel("incident-service"));
    }

    @Bean
    CourseServiceGrpc.CourseServiceBlockingStub courseServiceStub(GrpcChannelFactory channels) {
        return CourseServiceGrpc.newBlockingStub(channels.createChannel("course-service"));
    }

    @Bean
    ReviewServiceGrpc.ReviewServiceBlockingStub reviewServiceStub(GrpcChannelFactory channels) {
        return ReviewServiceGrpc.newBlockingStub(channels.createChannel("review-service"));
    }
}
