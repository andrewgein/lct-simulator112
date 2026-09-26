package com.simulator112.course.adapter.out.incident;

import com.simulator112.course.application.port.out.IncidentCatalogPort;
import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.CourseTargetType;
import com.simulator112.course.domain.exception.IncidentNotFoundException;
import com.simulator112.incident.grpc.contract.GetIncidentRequest;
import com.simulator112.incident.grpc.contract.IncidentGrpcServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IncidentGrpcAdapter implements IncidentCatalogPort {
    private final IncidentGrpcServiceGrpc.IncidentGrpcServiceBlockingStub stub;

    @Override
    public IncidentDescriptor requireIncident(UUID incidentId) {
        try {
            var incident = stub.withDeadlineAfter(2, TimeUnit.SECONDS).getIncident(
                    GetIncidentRequest.newBuilder().setIncidentId(incidentId.toString()).build());
            return new IncidentDescriptor(incidentId,
                    CourseTargetType.valueOf(incident.getTargetType().name().replace("INCIDENT_TARGET_TYPE_", "")),
                    AssignmentDifficulty.valueOf(incident.getDifficulty().name().replace("DIFFICULTY_", "")));
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new IncidentNotFoundException(incidentId);
            }
            throw exception;
        }
    }

    @Configuration
    static class GrpcConfiguration {
        @Bean(destroyMethod = "shutdown")
        ManagedChannel incidentChannel(
                @Value("${incident.grpc.host:localhost}") String host,
                @Value("${incident.grpc.port:9091}") int port) {
            return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        }

        @Bean
        IncidentGrpcServiceGrpc.IncidentGrpcServiceBlockingStub incidentStub(
                @Qualifier("incidentChannel") ManagedChannel channel) {
            return IncidentGrpcServiceGrpc.newBlockingStub(channel);
        }
    }
}
