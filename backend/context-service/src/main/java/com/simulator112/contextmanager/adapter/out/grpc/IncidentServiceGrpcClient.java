package com.simulator112.contextmanager.adapter.out.grpc;

import com.simulator112.contextmanager.adapter.grpc.mapper.IncidentContextMapper;
import com.simulator112.contextmanager.application.port.out.LevelCatalogPort;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.LevelScenario;
import com.simulator112.incident.grpc.contract.GetLevelRequest;
import com.simulator112.incident.grpc.contract.IncidentGrpcServiceGrpc;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncidentServiceGrpcClient implements LevelCatalogPort {
    private final IncidentGrpcServiceGrpc.IncidentGrpcServiceBlockingStub incidentServiceStub;

    @Override
    public LevelScenario getLevel(UUID levelId) {
        var level = incidentServiceStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getLevel(GetLevelRequest.newBuilder().setLevelId(levelId.toString()).build());
        return new LevelScenario(UUID.fromString(level.getId()), level.getTitle(),
                IncidentTargetType.valueOf(level.getTargetType().name().replace("INCIDENT_TARGET_TYPE_", "")),
                com.simulator112.shared.dto.Difficulty.valueOf(level.getDifficulty().name().replace("DIFFICULTY_", "")),
                ExecutionMode.valueOf(level.getExecutionMode().name().replace("EXECUTION_MODE_", "")),
                IntStream.range(0, level.getIncidentsCount())
                        .mapToObj(index -> IncidentContextMapper.toDomain(level.getIncidents(index), index)).toList());
    }
}
