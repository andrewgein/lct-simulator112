package com.simulator112.course.adapter.out.classifier;

import com.simulator112.classifier.grpc.contract.ClassifierServiceGrpc;
import com.simulator112.classifier.grpc.contract.HasDispatchServiceRequest;
import com.simulator112.course.application.port.out.DispatchServiceCatalogPort;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ClassifierGrpcAdapter implements DispatchServiceCatalogPort {
    private final ClassifierServiceGrpc.ClassifierServiceBlockingStub stub;

    @Override
    public void requireService(String serviceCode) {
        boolean exists = stub.withDeadlineAfter(2, TimeUnit.SECONDS).hasDispatchService(
                HasDispatchServiceRequest.newBuilder().setServiceCode(serviceCode).build()).getExists();
        if (!exists) throw new IllegalArgumentException("Служба не найдена в классификаторе: " + serviceCode);
    }

    @Configuration
    static class GrpcConfiguration {
        @Bean(destroyMethod = "shutdown")
        ManagedChannel classifierChannel(
                @Value("${classifier.grpc.host:localhost}") String host,
                @Value("${classifier.grpc.port:9093}") int port) {
            return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        }

        @Bean
        ClassifierServiceGrpc.ClassifierServiceBlockingStub classifierStub(
                @Qualifier("classifierChannel") ManagedChannel channel) {
            return ClassifierServiceGrpc.newBlockingStub(channel);
        }
    }
}
