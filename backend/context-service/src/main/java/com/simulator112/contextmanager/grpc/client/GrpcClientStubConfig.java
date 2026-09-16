package com.simulator112.contextmanager.grpc.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import com.simulator112.review.grpc.contract.ReviewServiceGrpc;
import com.simulator112.incident.grpc.contract.IncidentGrpcServiceGrpc;

@Configuration
public class GrpcClientStubConfig {

    @Bean
    IncidentGrpcServiceGrpc.IncidentGrpcServiceBlockingStub incidentServiceStub(GrpcChannelFactory channels) {
        return IncidentGrpcServiceGrpc.newBlockingStub(channels.createChannel("incident-service"));
    }

    @Bean
    ReviewServiceGrpc.ReviewServiceBlockingStub reviewServiceStub(GrpcChannelFactory channels) {
        return ReviewServiceGrpc.newBlockingStub(channels.createChannel("review-service"));
    }
}
