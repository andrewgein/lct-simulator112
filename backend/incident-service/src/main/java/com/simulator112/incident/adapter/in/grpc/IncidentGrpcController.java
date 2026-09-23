package com.simulator112.incident.adapter.in.grpc;

import com.simulator112.incident.application.port.in.FindAvailableIncidentsUseCase;
import com.simulator112.incident.application.port.in.GetIncidentUseCase;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.grpc.contract.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class IncidentGrpcController extends IncidentGrpcServiceGrpc.IncidentGrpcServiceImplBase {
    private final GetIncidentUseCase getIncident;
    private final FindAvailableIncidentsUseCase findAvailableIncidents;
    private final IncidentGrpcMapper mapper;

    @Override
    public void getIncident(GetIncidentRequest request, StreamObserver<IncidentContext> observer) {
        try {
            observer.onNext(mapper.toProto(getIncident.getIncident(UUID.fromString(request.getIncidentId()))));
            observer.onCompleted();
        } catch (IllegalArgumentException exception) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void findAvailableIncidents(FindAvailableIncidentsRequest request,
                                       StreamObserver<FindAvailableIncidentsResponse> observer) {
        var response = FindAvailableIncidentsResponse.newBuilder()
                .addAllIncidents(findAvailableIncidents.findAvailableIncidents(
                                targetType(request.getTargetType()), difficulty(request.getDifficulty())).stream()
                        .map(mapper::toProto).toList())
                .build();
        observer.onNext(response);
        observer.onCompleted();
    }

    private IncidentTargetType targetType(com.simulator112.incident.grpc.contract.IncidentTargetType value) {
        return switch (value) {
            case INCIDENT_TARGET_TYPE_SYSTEM_112 -> IncidentTargetType.SYSTEM_112;
            case INCIDENT_TARGET_TYPE_DDS -> IncidentTargetType.DDS;
            default -> throw new IllegalArgumentException("target_type обязателен");
        };
    }

    private Difficulty difficulty(com.simulator112.incident.grpc.contract.Difficulty value) {
        return switch (value) {
            case DIFFICULTY_EASY -> Difficulty.EASY;
            case DIFFICULTY_NORMAL -> Difficulty.NORMAL;
            case DIFFICULTY_HARD -> Difficulty.HARD;
            default -> throw new IllegalArgumentException("difficulty обязателен");
        };
    }
}
