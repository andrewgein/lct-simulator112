package com.simulator112.contextmanager.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.simulator112.contextmanager.domain.common.ReactionStatus;
import com.simulator112.contextmanager.domain.common.ReactionStatusEvent;
import com.simulator112.contextmanager.domain.common.ServiceReaction;
import com.simulator112.contextmanager.domain.dds.DdsStageDetails;
import com.simulator112.contextmanager.domain.dds.DdsCompletionTrigger;
import com.simulator112.contextmanager.domain.common.IncidentStatus;
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
    void movesToNextStageWhenScheduledTimePasses() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID rootId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, rootId,
                List.of(stage(rootId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE),
                        stage(nextId, DdsStageType.COMPLETE_INCIDENT, StageStatus.PENDING)));
        context.getIncidents().add(incident);
        accept(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        incident.getStages().getFirst().setDeadlineAt(Instant.now().minusSeconds(1));
        var progress = service.getProgress(contextId);

        assertThat(progress.incidents().getFirst().dds().activeStageId()).isEqualTo(nextId);
        assertThat(incident.getStages().getFirst().getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(incident.getStages().get(1).getStatus()).isEqualTo(StageStatus.ACTIVE);
    }

    @Test
    void acceptingAfterFirstDeadlineStartsNextStageWithFreshTimer() {
        UUID contextId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot first = stage(firstId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE);
        first.setDeadlineAt(Instant.now().minusSeconds(10));
        IncidentSnapshot incident = incident(UUID.randomUUID(), firstId, List.of(first,
                stage(nextId, DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, StageStatus.PENDING)));
        incident.setInitialAssignmentService("MCHS");
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.getProgress(contextId);
        assertThat(incident.getActiveStageId()).isEqualTo(firstId);

        var progress = service.applyReactionStatus(contextId, incident.getSourceId(), "MCHS",
                ReactionStatus.ACCEPTED, null);

        assertThat(progress.incidents().getFirst().dds().activeStageId()).isEqualTo(nextId);
        StageSnapshot next = incident.getStages().get(1);
        assertThat(progress.incidents().getFirst().dds().deadline()).isEqualTo(next.getDeadlineAt());
        assertThat(next.getDeadlineAt()).isEqualTo(next.getStartedAt().plusSeconds(60));
        assertThat(next.getStartedAt()).isAfter(first.getDeadlineAt());
    }

    @Test
    void endOfTimelineCompletesIncident() {
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

        assertThat(incident.getStatus()).isEqualTo(IncidentProgressStatus.COMPLETED);
        assertThat(root.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
    }

    @Test
    void catchesUpMultipleStagesWithoutResettingTheirClock() {
        UUID contextId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot first = stage(firstId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE);
        first.setDeadlineAt(Instant.now().minusSeconds(90));
        StageSnapshot second = stage(secondId, DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, StageStatus.PENDING);
        IncidentSnapshot incident = incident(UUID.randomUUID(), firstId, List.of(first, second));
        context.getIncidents().add(incident);
        accept(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var progress = service.getProgress(contextId);

        assertThat(first.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(second.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(progress.incidents().getFirst().status()).isEqualTo(IncidentProgressStatus.COMPLETED);
        assertThat(second.getStartedAt()).isEqualTo(first.getDeadlineAt());
    }

    @Test
    void sequentialIncidentStartsAtStageWithLowestPosition() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        context.setExecutionMode(ExecutionMode.SEQUENTIAL);
        UUID activeId = UUID.randomUUID();
        StageSnapshot activeStage = stage(activeId, DdsStageType.COMPLETE_INCIDENT, StageStatus.ACTIVE);
        activeStage.setDeadlineAt(Instant.now().minusSeconds(1));
        IncidentSnapshot active = incident(UUID.randomUUID(), activeId, List.of(activeStage));
        context.getIncidents().add(active);
        accept(active);
        StageSnapshot later = stage(UUID.randomUUID(), DdsStageType.COMPLETE_INCIDENT, StageStatus.PENDING);
        StageSnapshot first = stage(UUID.randomUUID(), DdsStageType.ASSIGN_BRIGADE, StageStatus.PENDING);
        IncidentSnapshot pending = incident(UUID.randomUUID(), first.getSourceId(), List.of(first, later));
        pending.setStatus(IncidentProgressStatus.PENDING);
        pending.setPosition(1);
        pending.setActiveStageId(null);
        pending.setInitialAssignmentService("MCHS");
        pending.getStages().clear();
        pending.getStages().add(later);
        pending.getStages().add(first);
        context.getIncidents().add(pending);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.getProgress(contextId);

        assertThat(pending.getActiveStageId()).isEqualTo(first.getSourceId());
        assertThat(first.getStatus()).isEqualTo(StageStatus.ACTIVE);
        assertThat(later.getStatus()).isEqualTo(StageStatus.PENDING);
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
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        root.setDeadlineAt(Instant.now().minusSeconds(1));
        service.getProgress(contextId);

        StageSnapshot next = incident.getStages().get(1);
        assertThat(next.getDeadlineAt()).isEqualTo(next.getStartedAt().plusSeconds(60));
    }

    @Test
    void acceptanceImmediatelyStartsNextStage() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID responseId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, assignmentId,
                List.of(stage(assignmentId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE),
                        stage(responseId, DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, StageStatus.PENDING)));
        ServiceReaction reaction = new ServiceReaction("MCHS");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ADDED, Instant.now(), null));
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        incident.setInitialAssignmentService("MCHS");
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var progress = service.applyReactionStatus(
                contextId, incidentId, "MCHS", ReactionStatus.ACCEPTED, null);

        assertThat(progress.incidents().getFirst().serviceReactions().getFirst().currentStatus())
                .isEqualTo(ReactionStatus.ACCEPTED);
        assertThat(progress.incidents().getFirst().serviceReactions().getFirst().history()).hasSize(3);
        assertThat(incident.getActiveStageId()).isEqualTo(responseId);
        assertThat(incident.getStages().getFirst().getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(incident.getStages().get(1).getStatus()).isEqualTo(StageStatus.ACTIVE);
        assertThat(incident.getStages().get(1).getDeadlineAt())
                .isEqualTo(incident.getStages().get(1).getStartedAt().plusSeconds(60));
    }

    @Test
    void statusCanBeRecordedAfterTimelineCompletesBeforeReview() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, UUID.randomUUID(), List.of());
        incident.setStatus(IncidentProgressStatus.COMPLETED);
        incident.setActiveStageId(null);
        incident.setInitialAssignmentService("MCHS");
        ServiceReaction reaction = new ServiceReaction("MCHS");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyReactionStatus(contextId, incidentId, "MCHS", ReactionStatus.ACCEPTED, null);

        assertThat(reaction.currentStatus()).isEqualTo(ReactionStatus.ACCEPTED);
        assertThat(incident.getStatus()).isEqualTo(IncidentProgressStatus.COMPLETED);
        context.setStatus(ContextStatus.IN_REVIEW);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.applyReactionStatus(
                contextId, incidentId, "MCHS", ReactionStatus.RESPONSE_STARTED, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Разбор");
    }

    @Test
    void requiresCommentWhenReactionIsRejected() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, assignmentId,
                List.of(stage(assignmentId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE)));
        ServiceReaction reaction = new ServiceReaction("MCHS");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        incident.setInitialAssignmentService("MCHS");
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.applyReactionStatus(
                contextId, incidentId, "MCHS", ReactionStatus.NOT_ACCEPTED, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("комментарий");
    }

    @Test
    void workRefusalCanBeRecordedWithReason() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        IncidentSnapshot incident = incident(incidentId, stageId,
                List.of(stage(stageId, DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, StageStatus.ACTIVE)));
        incident.setInitialAssignmentService("MCHS");
        ServiceReaction reaction = new ServiceReaction("MCHS");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ACCEPTED, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var progress = service.applyReactionStatus(contextId, incidentId, "MCHS",
                ReactionStatus.WORK_REFUSED, "Нет доступа к месту работ");

        assertThat(progress.incidents().getFirst().serviceReactions().getFirst().currentStatus())
                .isEqualTo(ReactionStatus.WORK_REFUSED);
        assertThat(reaction.getHistory().getLast().comment()).isEqualTo("Нет доступа к месту работ");
        assertThat(incident.getActiveStageId()).isNull();
        assertThat(incident.getStatus()).isEqualTo(IncidentProgressStatus.FAILED);
    }

    @Test
    void statusTriggerAdvancesWithoutWaitingForTimer() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot first = stage(UUID.randomUUID(), DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, StageStatus.ACTIVE);
        first.getDds().setCompletionTriggers(List.of(DdsCompletionTrigger.STATUS));
        first.getDds().setActualStatus(IncidentStatus.RESPONSE_STARTED);
        StageSnapshot next = stage(UUID.randomUUID(), DdsStageType.COMPLETE_INCIDENT, StageStatus.PENDING);
        IncidentSnapshot incident = incident(UUID.randomUUID(), first.getSourceId(), List.of(first, next));
        incident.setInitialAssignmentService("MCHS");
        accept(incident);
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyReactionStatus(contextId, incident.getSourceId(), "MCHS", ReactionStatus.RESPONSE_STARTED, null);

        assertThat(incident.getActiveStageId()).isEqualTo(next.getSourceId());
        assertThat(first.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
    }

    @Test
    void callTriggerWaitsForAllCallsAndDoesNotExpireOnTime() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot first = stage(UUID.randomUUID(), DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.ACTIVE);
        first.getDds().setCompletionTriggers(List.of(DdsCompletionTrigger.CALLS));
        first.setDeadlineAt(null);
        CallSnapshot a = new CallSnapshot(); a.setSourceId(UUID.randomUUID()); a.setStatus(CallStatus.COMPLETED);
        CallSnapshot b = new CallSnapshot(); b.setSourceId(UUID.randomUUID()); b.setStatus(CallStatus.ACTIVE);
        first.getCalls().addAll(List.of(a, b));
        StageSnapshot next = stage(UUID.randomUUID(), DdsStageType.COMPLETE_INCIDENT, StageStatus.PENDING);
        IncidentSnapshot incident = incident(UUID.randomUUID(), first.getSourceId(), List.of(first, next));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.getProgress(contextId);
        assertThat(incident.getActiveStageId()).isEqualTo(first.getSourceId());
        b.setStatus(CallStatus.COMPLETED);
        service.getProgress(contextId);
        assertThat(incident.getActiveStageId()).isEqualTo(next.getSourceId());
    }

    @Test
    void timeTriggerAdvancesEvenWithUnfinishedCalls() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot first = stage(UUID.randomUUID(), DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.ACTIVE);
        first.getDds().setCompletionTriggers(List.of(DdsCompletionTrigger.TIME, DdsCompletionTrigger.CALLS));
        first.setDeadlineAt(Instant.now().minusSeconds(1));
        CallSnapshot call = new CallSnapshot(); call.setSourceId(UUID.randomUUID()); call.setStatus(CallStatus.PENDING);
        first.getCalls().add(call);
        StageSnapshot next = stage(UUID.randomUUID(), DdsStageType.COMPLETE_INCIDENT, StageStatus.PENDING);
        IncidentSnapshot incident = incident(UUID.randomUUID(), first.getSourceId(), List.of(first, next));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.getProgress(contextId);

        assertThat(incident.getActiveStageId()).isEqualTo(next.getSourceId());
        assertThat(call.getStatus()).isEqualTo(CallStatus.PENDING);
    }

    @Test
    void refusalStopsOnlyCurrentIncident() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot stage = stage(UUID.randomUUID(), DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE);
        IncidentSnapshot refused = incident(UUID.randomUUID(), stage.getSourceId(), List.of(stage));
        refused.setInitialAssignmentService("MCHS");
        StageSnapshot otherStage = stage(UUID.randomUUID(), DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, StageStatus.ACTIVE);
        IncidentSnapshot other = incident(UUID.randomUUID(), otherStage.getSourceId(), List.of(otherStage));
        context.getIncidents().addAll(List.of(refused, other));
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyReactionStatus(contextId, refused.getSourceId(), "MCHS", ReactionStatus.NOT_ACCEPTED, "Отказ");

        assertThat(refused.getStatus()).isEqualTo(IncidentProgressStatus.FAILED);
        assertThat(other.getStatus()).isEqualTo(IncidentProgressStatus.ACTIVE);
    }

    @Test
    void commentOnCallDoesNotAdvanceStage() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID callStageId = UUID.randomUUID();
        UUID nextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot callStage = stage(callStageId, DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.ACTIVE);
        CallSnapshot call = new CallSnapshot();
        call.setSourceId(UUID.randomUUID());
        call.setStatus(CallStatus.DISCONNECTED);
        callStage.getCalls().add(call);
        IncidentSnapshot incident = incident(incidentId, callStageId, List.of(callStage,
                stage(nextId, DdsStageType.COMPLETE_INCIDENT, StageStatus.PENDING)));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.saveDdsComment(
                contextId, incidentId, callStageId, "  "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("результат");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.saveDdsComment(
                contextId, incidentId, callStageId, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("результат");

        var progress = service.saveDdsComment(contextId, incidentId, callStageId, "  Бригада прибыла  ");

        assertThat(callStage.getDds().getComment()).isEqualTo("Бригада прибыла");
        assertThat(callStage.getStatus()).isEqualTo(StageStatus.ACTIVE);
        assertThat(progress.incidents().getFirst().dds().activeStageId()).isEqualTo(callStageId);
    }

    @Test
    void rejectsCommentOnStageWithoutExpectedComment() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        context.getIncidents().add(incident(incidentId, stageId,
                List.of(stage(stageId, DdsStageType.ASSIGN_BRIGADE, StageStatus.ACTIVE))));
        when(repository.findById(contextId)).thenReturn(Optional.of(context));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.saveDdsComment(
                contextId, incidentId, stageId, "Бригада прибыла"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("не предусмотрен");
    }

    @Test
    void storesCommentAfterTimelineCompletesWithoutCompletedCall() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot stage = stage(stageId, DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.SUCCEEDED);
        stage.getCalls().add(new CallSnapshot());
        IncidentSnapshot incident = incident(incidentId, stageId, List.of(stage));
        incident.setStatus(IncidentProgressStatus.COMPLETED);
        incident.setActiveStageId(null);
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveDdsComment(contextId, incidentId, stageId, "Бригада сообщила о прибытии");

        assertThat(stage.getDds().getComment()).isEqualTo("Бригада сообщила о прибытии");
        assertThat(incident.getStatus()).isEqualTo(IncidentProgressStatus.COMPLETED);
    }

    @Test
    void callCommentCannotAdvanceStageAfterDeadline() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot callStage = stage(stageId, DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.ACTIVE);
        callStage.setDeadlineAt(Instant.now().minusSeconds(1));
        CallSnapshot call = new CallSnapshot();
        call.setSourceId(UUID.randomUUID());
        call.setStatus(CallStatus.COMPLETED);
        callStage.getCalls().add(call);
        IncidentSnapshot incident = incident(incidentId, stageId, List.of(callStage));
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var progress = service.saveDdsComment(contextId, incidentId, stageId, "Бригада прибыла");

        assertThat(callStage.getStatus()).isEqualTo(StageStatus.ACTIVE);
        assertThat(callStage.getDds().getComment()).isEqualTo("Бригада прибыла");
        assertThat(progress.incidents().getFirst().status()).isEqualTo(IncidentProgressStatus.ACTIVE);
    }

    @Test
    void confirmingOneIncidentDoesNotAdvanceAnother() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        StageSnapshot first = stage(UUID.randomUUID(), DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.ACTIVE);
        StageSnapshot second = stage(UUID.randomUUID(), DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.ACTIVE);
        CallSnapshot call = new CallSnapshot();
        call.setSourceId(UUID.randomUUID());
        call.setStatus(CallStatus.COMPLETED);
        first.getCalls().add(call);
        context.getIncidents().add(incident(firstId, first.getSourceId(), List.of(first)));
        context.getIncidents().add(incident(secondId, second.getSourceId(), List.of(second)));
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveDdsComment(contextId, firstId, first.getSourceId(), "Бригада прибыла");

        assertThat(first.getStatus()).isEqualTo(StageStatus.ACTIVE);
        assertThat(second.getStatus()).isEqualTo(StageStatus.ACTIVE);
        assertThat(second.getDds().getComment()).isNull();
    }

    @Test
    void arrivalDoesNotFinishCallStage() {
        UUID contextId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TrainingContext context = context(contextId);
        StageSnapshot callStage = stage(stageId, DdsStageType.CALL_BRIGADE_FOR_STATUS, StageStatus.ACTIVE);
        IncidentSnapshot incident = incident(incidentId, stageId, List.of(callStage));
        incident.setInitialAssignmentService("MCHS");
        ServiceReaction reaction = new ServiceReaction("MCHS");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RESPONSE_STARTED, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
        context.getIncidents().add(incident);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyReactionStatus(contextId, incidentId, "MCHS", ReactionStatus.ARRIVED, null);

        assertThat(callStage.getStatus()).isEqualTo(StageStatus.ACTIVE);
        assertThat(incident.getActiveStageId()).isEqualTo(stageId);
    }

    @Test
    void system112ContextCannotChangeReactionStatus() {
        UUID contextId = UUID.randomUUID();
        TrainingContext context = new TrainingContext();
        context.setId(contextId);
        context.setTargetType(IncidentTargetType.SYSTEM_112);
        when(repository.findById(contextId)).thenReturn(Optional.of(context));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.applyReactionStatus(
                contextId, UUID.randomUUID(), "MCHS", ReactionStatus.ACCEPTED, null))
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

    private void accept(IncidentSnapshot incident) {
        ServiceReaction reaction = new ServiceReaction("MCHS");
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ACCEPTED, Instant.now(), null));
        incident.getServiceReactions().add(reaction);
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
        incident.setActiveStageId(activeStageId);
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        for (int index = 0; index < stages.size(); index++) {
            stages.get(index).setPosition(index);
            incident.getStages().add(stages.get(index));
        }
        return incident;
    }

    private StageSnapshot stage(UUID id, DdsStageType type, StageStatus status) {
        StageSnapshot stage = new StageSnapshot();
        stage.setSourceId(id);
        stage.setDds(new DdsStageDetails(type, 60,
                type == DdsStageType.CALL_BRIGADE_FOR_STATUS ? "Бригада прибыла" : null, null));
        stage.setStatus(status);
        if (status == StageStatus.ACTIVE) {
            stage.setStartedAt(Instant.now());
            stage.setDeadlineAt(Instant.now().plusSeconds(60));
        }
        return stage;
    }
}
