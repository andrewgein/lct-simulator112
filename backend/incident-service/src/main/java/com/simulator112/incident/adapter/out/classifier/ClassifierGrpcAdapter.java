package com.simulator112.incident.adapter.out.classifier;

import com.simulator112.classifier.grpc.contract.ClassifierServiceGrpc;
import com.simulator112.classifier.grpc.contract.GetClassifierEntryRequest;
import com.simulator112.classifier.grpc.contract.HasDispatchServiceRequest;
import com.simulator112.classifier.grpc.contract.SearchClassifierEntriesRequest;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.domain.common.exception.ClassifierEntryNotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ClassifierGrpcAdapter implements ClassifierCatalogPort {
    private final ClassifierServiceGrpc.ClassifierServiceBlockingStub stub;

    @Override
    public void requireEntry(String classifierCode) {
        try {
            stub.withDeadlineAfter(2, TimeUnit.SECONDS).getClassifierEntry(
                    GetClassifierEntryRequest.newBuilder().setClassifierCode(classifierCode).build());
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new ClassifierEntryNotFoundException(classifierCode);
            }
            throw exception;
        }
    }

    @Override
    public void requireService(String serviceCode) {
        boolean exists = stub.withDeadlineAfter(2, TimeUnit.SECONDS).hasDispatchService(
                HasDispatchServiceRequest.newBuilder().setServiceCode(serviceCode).build()).getExists();
        if (!exists) throw new IllegalArgumentException("Служба не найдена в классификаторе: " + serviceCode);
    }

    @Override
    public List<Candidate> search(String query, int limit, List<String> includedCodes) {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS).searchClassifierEntries(
                SearchClassifierEntriesRequest.newBuilder().setQuery(query).setLimit(limit)
                        .addAllIncludedCodes(includedCodes).build()).getEntriesList().stream()
                .map(entry -> new Candidate(entry.getCode(), entry.getCategoryName(), entry.getFinalName()))
                .toList();
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
        ClassifierServiceGrpc.ClassifierServiceBlockingStub classifierStub(ManagedChannel channel) {
            return ClassifierServiceGrpc.newBlockingStub(channel);
        }
    }
}
