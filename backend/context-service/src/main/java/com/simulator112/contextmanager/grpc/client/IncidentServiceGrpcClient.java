package com.simulator112.contextmanager.grpc.client;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.simulator112.incident.grpc.contract.GetIncidentContextRequest;
import com.simulator112.incident.grpc.contract.IncidentGrpcServiceGrpc;
import com.simulator112.incident.grpc.contract.LevelContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IncidentServiceGrpcClient {

    private final IncidentGrpcServiceGrpc.IncidentGrpcServiceBlockingStub incidentServiceStub;

    public LevelContext getLevelContext(UUID levelId) {
        GetIncidentContextRequest request = GetIncidentContextRequest.newBuilder()
                .setLevelId(levelId.toString())
                .build();

        return incidentServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .getIncidentContext(request);
    }
}
