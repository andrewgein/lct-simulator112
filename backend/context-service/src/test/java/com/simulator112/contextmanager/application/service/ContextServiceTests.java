package com.simulator112.contextmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.application.port.out.LevelCatalogPort;
import com.simulator112.contextmanager.application.port.out.ReviewPort;
import com.simulator112.contextmanager.adapter.grpc.mapper.IncidentContextMapper;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.common.LevelScenario;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.incident.grpc.contract.DdsStageDetails;
import com.simulator112.incident.grpc.contract.DdsStageType;
import com.simulator112.incident.grpc.contract.Difficulty;
import com.simulator112.incident.grpc.contract.ExecutionMode;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.IncidentStage;
import com.simulator112.incident.grpc.contract.IncidentTargetType;
import com.simulator112.incident.grpc.contract.LevelContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContextServiceTests {
    private final ContextStore repository = mock(ContextStore.class);
    private final ReviewPort review = mock(ReviewPort.class);
    private final LevelCatalogPort incidentService = mock(LevelCatalogPort.class);
    private final ContextService service = new ContextService(repository, review, incidentService);

    @Test
    void startsAllDdsIncidentsInParallelMode() {
        UUID levelId = UUID.randomUUID();
        LevelContext level = LevelContext.newBuilder()
                .setId(levelId.toString())
                .setTitle("Параллельная ДДС")
                .setTargetType(IncidentTargetType.INCIDENT_TARGET_TYPE_DDS)
                .setDifficulty(Difficulty.DIFFICULTY_NORMAL)
                .setExecutionMode(ExecutionMode.EXECUTION_MODE_PARALLEL)
                .addIncidents(ddsIncident())
                .addIncidents(ddsIncident())
                .build();
        var scenario = new LevelScenario(levelId, level.getTitle(),
                com.simulator112.contextmanager.domain.common.IncidentTargetType.DDS,
                com.simulator112.shared.dto.Difficulty.NORMAL,
                com.simulator112.contextmanager.domain.common.ExecutionMode.PARALLEL,
                java.util.stream.IntStream.range(0, level.getIncidentsCount())
                        .mapToObj(index -> IncidentContextMapper.toDomain(level.getIncidents(index), index)).toList());
        when(incidentService.getLevel(levelId)).thenReturn(scenario);
        when(repository.save(any())).thenAnswer(invocation -> {
            TrainingContext context = invocation.getArgument(0);
            context.setId(UUID.randomUUID());
            return context;
        });

        TrainingContext context = service.create(UUID.randomUUID(), levelId);

        assertThat(context.getIncidents())
                .allMatch(incident -> incident.getStatus() == IncidentProgressStatus.ACTIVE);
        assertThat(context.getIncidents())
                .allMatch(incident -> incident.getStages().getFirst().getStatus() == StageStatus.ACTIVE);
        assertThat(context.getIncidents())
                .allMatch(incident -> incident.getStages().getFirst().getDeadlineAt()
                        .equals(incident.getStages().getFirst().getStartedAt().plusSeconds(60)));
    }

    private IncidentContext ddsIncident() {
        UUID stageId = UUID.randomUUID();
        return IncidentContext.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTitle("Пожар")
                .setTargetType(IncidentTargetType.INCIDENT_TARGET_TYPE_DDS)
                .setDifficulty(Difficulty.DIFFICULTY_NORMAL)
                .setDdsInitialStageId(stageId.toString())
                .addStages(IncidentStage.newBuilder()
                        .setId(stageId.toString())
                        .setTitle("Назначение")
                        .setDds(DdsStageDetails.newBuilder()
                                .setType(DdsStageType.DDS_STAGE_TYPE_ASSIGN_BRIGADE)
                                .setTimeLimitSeconds(60)))
                .build();
    }
}
