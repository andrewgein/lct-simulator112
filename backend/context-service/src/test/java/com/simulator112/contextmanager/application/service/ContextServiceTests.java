package com.simulator112.contextmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.application.port.out.CourseAssignmentPort;
import com.simulator112.contextmanager.application.port.out.ReviewPort;
import com.simulator112.contextmanager.adapter.grpc.mapper.IncidentContextMapper;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.AssignmentScenario;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.incident.grpc.contract.DdsStageDetails;
import com.simulator112.incident.grpc.contract.DdsStageType;
import com.simulator112.incident.grpc.contract.Difficulty;
import com.simulator112.incident.grpc.contract.ExecutionMode;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.IncidentStage;
import com.simulator112.incident.grpc.contract.IncidentTargetType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContextServiceTests {
    private final ContextStore repository = mock(ContextStore.class);
    private final ReviewPort review = mock(ReviewPort.class);
    private final CourseAssignmentPort courseAssignments = mock(CourseAssignmentPort.class);
    private final ContextService service = new ContextService(repository, review, courseAssignments);

    @Test
    void startsAllDdsIncidentsInParallelMode() {
        UUID assignmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var incidentProtos = java.util.List.of(ddsIncident(), ddsIncident());
        var scenario = new AssignmentScenario(assignmentId, userId, "Параллельная ДДС",
                com.simulator112.contextmanager.domain.common.IncidentTargetType.DDS,
                com.simulator112.shared.dto.Difficulty.NORMAL,
                com.simulator112.contextmanager.domain.common.ExecutionMode.PARALLEL,
                java.util.stream.IntStream.range(0, incidentProtos.size())
                        .mapToObj(index -> IncidentContextMapper.toDomain(incidentProtos.get(index), index)).toList(),
                40, 60, 80);
        when(courseAssignments.getAssignmentForUser(assignmentId, userId)).thenReturn(scenario);
        when(repository.save(any())).thenAnswer(invocation -> {
            TrainingContext context = invocation.getArgument(0);
            context.setId(UUID.randomUUID());
            return context;
        });

        TrainingContext context = service.create(userId, assignmentId);

        assertThat(context.getThreshold3()).isEqualTo(40);
        assertThat(context.getThreshold4()).isEqualTo(60);
        assertThat(context.getThreshold5()).isEqualTo(80);
        var call = new com.simulator112.contextmanager.domain.common.CallSnapshot();
        call.setSourceId(UUID.randomUUID());
        call.setStatus(com.simulator112.contextmanager.domain.common.CallStatus.COMPLETED);
        call.setPosition(0);
        call.setDirection(com.simulator112.contextmanager.domain.common.CallDirection.OUTBOUND);
        call.setCounterparty(com.simulator112.contextmanager.domain.common.CounterpartyType.BRIGADE);
        context.getIncidents().getFirst().getStages().getFirst().getCalls().add(call);
        var proto = com.simulator112.contextmanager.adapter.grpc.mapper.FullContextMapper.toProto(context);
        assertThat(proto.getAssignmentContext().getThreshold3()).isEqualTo(40);
        assertThat(proto.getAssignmentContext().getThreshold4()).isEqualTo(60);
        assertThat(proto.getAssignmentContext().getThreshold5()).isEqualTo(80);
        assertThat(proto.getAssignmentContext().getIncidents(0).getStages(0).getDds().getActualStatus())
                .isEqualTo(com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_ARRIVED);
        assertThat(proto.getLevelProgress().getIncidents(0).getReactionEventsList())
                .extracting(com.simulator112.context.grpc.contract.ReactionEvent::getStatus)
                .contains(com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_RECEIVED_BY_SERVICE);
        assertThat(proto.getLevelProgress().getIncidents(0).getDds().getStages(0).getCompletedCallIdsList())
                .containsExactly(call.getSourceId().toString());
        assertThat(context.getIncidents())
                .allMatch(incident -> incident.getStatus() == IncidentProgressStatus.ACTIVE);
        assertThat(context.getIncidents())
                .allMatch(incident -> incident.getStages().getFirst().getStatus() == StageStatus.ACTIVE);
        assertThat(context.getIncidents())
                .allMatch(incident -> incident.getStages().getFirst().getDeadlineAt()
                        .equals(incident.getStages().getFirst().getStartedAt().plusSeconds(60)));
    }

    @Test
    void automaticReviewCompletesContextWithoutExpertConfirmation() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = new TrainingContext();
        context.setId(contextId);
        context.setStatus(ContextStatus.FILLED);
        context.setTargetType(com.simulator112.contextmanager.domain.common.IncidentTargetType.DDS);
        var incident = new com.simulator112.contextmanager.domain.common.IncidentSnapshot();
        incident.setStatus(IncidentProgressStatus.COMPLETED);
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(review.send(any())).thenReturn(true);

        service.closeContext(contextId);

        assertThat(context.getStatus()).isEqualTo(ContextStatus.DONE);
        verify(review).send(any());
    }

    private IncidentContext ddsIncident() {
        UUID stageId = UUID.randomUUID();
        return IncidentContext.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTitle("Пожар")
                .setTargetType(IncidentTargetType.INCIDENT_TARGET_TYPE_DDS)
                .setDifficulty(Difficulty.DIFFICULTY_NORMAL)
                .addStages(IncidentStage.newBuilder()
                        .setId(stageId.toString())
                        .setTitle("Назначение")
                        .setDds(DdsStageDetails.newBuilder()
                                .setType(DdsStageType.DDS_STAGE_TYPE_ASSIGN_BRIGADE)
                                .setTimeLimitSeconds(60)
                                .setActualStatus(com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_ARRIVED)))
                .build();
    }
}
