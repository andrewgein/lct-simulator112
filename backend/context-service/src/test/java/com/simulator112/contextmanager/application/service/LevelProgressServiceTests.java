package com.simulator112.contextmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.simulator112.contextmanager.domain.common.ReactionStatus;
import com.simulator112.contextmanager.domain.common.ReactionStatusEvent;
import com.simulator112.contextmanager.domain.common.ServiceReaction;
import com.simulator112.contextmanager.domain.dds.DdsStageSignal;
import com.simulator112.contextmanager.domain.dds.DdsStageTransition;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.dds.DdsStageType;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.StageStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LevelProgressServiceTests {
    private final ContextStore repository = mock(ContextStore.class);
    private final LevelProgressService service = new LevelProgressService(repository);

    @Test
    void movesToSuccessStageBeforeDeadline() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID rootId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, rootId,
                List.of(stage(rootId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE),
                        stage(nextId, DdsStageType.COMPLETE_INCIDENT, StageStatus.PENDING)));
        incident.setTransitions(new ArrayList<>(List.of(
                new DdsStageTransition(rootId, nextId, null))));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var progress = service.applyDdsSignal(contextId, incidentId, DdsStageSignal.BRIGADE_ASSIGNED);

        assertThat(progress.incidents().getFirst().dds().activeStageId()).isEqualTo(nextId);
        assertThat(incident.getStages().getFirst().getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(incident.getStages().get(1).getStatus()).isEqualTo(StageStatus.ACTIVE);
    }

    @Test
    void timeoutUsesFailureOutcome() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID rootId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot root = stage(rootId, DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE,
                StageStatus.ACTIVE);
        root.setDeadlineAt(Instant.now().minusSeconds(1));
        IncidentSnapshot incident = incident(incidentId, rootId, List.of(root));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.getProgress(contextId);

        assertThat(incident.getStatus()).isEqualTo(IncidentProgressStatus.FAILED);
        assertThat(root.getStatus()).isEqualTo(StageStatus.TIMED_OUT);
    }

    @Test
    void stageUsesConfiguredDeadline() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID rootId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot root = stage(rootId, DdsStageType.COMPLETE_INCIDENT, StageStatus.ACTIVE);
        IncidentSnapshot incident = incident(incidentId, rootId,
                List.of(root, stage(nextId, DdsStageType.ASSIGN_BRIGADE, StageStatus.PENDING)));
        incident.setTransitions(new ArrayList<>(List.of(new DdsStageTransition(rootId, nextId, null))));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyDdsSignal(contextId, incidentId, DdsStageSignal.INCIDENT_COMPLETED);

        StageSnapshot next = incident.getStages().get(1);
        assertThat(next.getDeadlineAt()).isEqualTo(next.getStartedAt().plusSeconds(60));
    }

    @Test
    void wrongSignalUsesFailureOutcome() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID rootId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, rootId,
                List.of(stage(rootId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE)));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyDdsSignal(contextId, incidentId, DdsStageSignal.INCIDENT_COMPLETED);

        assertThat(incident.getStatus()).isEqualTo(IncidentProgressStatus.FAILED);
        assertThat(incident.getActiveStageId()).isNull();
    }

    @Test
    void storesReactionStatusHistoryAndAdvancesMatchingStage() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID responseId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, assignmentId,
                List.of(stage(assignmentId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE),
                        stage(responseId, DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, StageStatus.PENDING)));
        ServiceReaction reaction = new ServiceReaction("FIRE");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ADDED, Instant.now(), null));
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        incident.setInitialAssignmentService("FIRE");
        incident.setTransitions(new ArrayList<>(List.of(new DdsStageTransition(assignmentId, responseId, null))));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var progress = service.applyReactionStatus(
                contextId, incidentId, "FIRE", ReactionStatus.ACCEPTED, null);

        assertThat(progress.incidents().getFirst().serviceReactions().getFirst().currentStatus())
                .isEqualTo(ReactionStatus.ACCEPTED);
        assertThat(progress.incidents().getFirst().serviceReactions().getFirst().history()).hasSize(3);
        assertThat(incident.getActiveStageId()).isEqualTo(responseId);
    }

    @Test
    void requiresCommentWhenReactionIsRejected() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, assignmentId,
                List.of(stage(assignmentId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE)));
        ServiceReaction reaction = new ServiceReaction("FIRE");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        incident.setInitialAssignmentService("FIRE");
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.applyReactionStatus(
                contextId, incidentId, "FIRE", ReactionStatus.NOT_ACCEPTED, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("комментарий");
    }

    @Test
    void system112ContextCannotChangeReactionStatus() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = new TrainingContext();
        context.setId(contextId);
        context.setTargetType(IncidentTargetType.SYSTEM_112);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.applyReactionStatus(
                contextId, UUID.randomUUID(), "FIRE", ReactionStatus.ACCEPTED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DDS");
    }

    @Test
    void returnsSystem112CallProgress() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = new TrainingContext();
        context.setId(contextId);
        context.setTargetType(IncidentTargetType.SYSTEM_112);
        context.setExecutionMode(ExecutionMode.SEQUENTIAL);
        context.setStatus(ContextStatus.DIALOG);
        IncidentSnapshot incident = new IncidentSnapshot();
        incident.setSourceId(UUID.randomUUID());
        incident.setPosition(0);
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        StageSnapshot stage = new StageSnapshot();
        stage.setSourceId(UUID.randomUUID());
        stage.setStatus(StageStatus.ACTIVE);
        CallSnapshot completed = new CallSnapshot();
        completed.setSourceId(UUID.randomUUID());
        completed.setStatus(CallStatus.COMPLETED);
        CallSnapshot active = new CallSnapshot();
        active.setSourceId(UUID.randomUUID());
        active.setStatus(CallStatus.ACTIVE);
        stage.getCalls().add(completed);
        stage.getCalls().add(active);
        incident.getStages().add(stage);
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));

        var progress = service.getProgress(contextId);

        assertThat(progress.targetType()).isEqualTo(IncidentTargetType.SYSTEM_112);
        assertThat(progress.incidents().getFirst().system112().activeCallId())
                .isEqualTo(active.getSourceId());
        assertThat(progress.incidents().getFirst().system112().completedCalls()).isEqualTo(1);
        assertThat(progress.incidents().getFirst().system112().totalCalls()).isEqualTo(2);
    }

    private TrainingContext context(UUID id) {
        TrainingContext context = new TrainingContext();
        context.setId(id);
        context.setTargetType(IncidentTargetType.DDS);
        context.setExecutionMode(ExecutionMode.PARALLEL);
        return context;
    }

    private IncidentSnapshot incident(UUID id, UUID activeStageId, List<StageSnapshot> stages) {
        IncidentSnapshot incident = new IncidentSnapshot();
        incident.setSourceId(id);
        incident.setPosition(0);
        incident.setInitialStageId(activeStageId);
        incident.setActiveStageId(activeStageId);
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        stages.forEach(incident.getStages()::add);
        return incident;
    }

    private StageSnapshot stage(UUID id, DdsStageType type, StageStatus status) {
        StageSnapshot stage = new StageSnapshot();
        stage.setSourceId(id);
        stage.setDdsStageType(type);
        stage.setTimeLimitSeconds(60);
        stage.setStatus(status);
        if (status == StageStatus.ACTIVE) {
            stage.setStartedAt(Instant.now());
            stage.setDeadlineAt(Instant.now().plusSeconds(60));
        }
        return stage;
    }
}
