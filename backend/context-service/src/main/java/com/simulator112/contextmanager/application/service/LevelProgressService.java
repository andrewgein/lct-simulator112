package com.simulator112.contextmanager.application.service;

import com.simulator112.contextmanager.application.port.in.LevelProgressUseCase;
import com.simulator112.contextmanager.application.port.in.ProcessDdsTimeoutsUseCase;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.dds.DdsProgress;
import com.simulator112.contextmanager.domain.dds.DdsStageProgress;
import com.simulator112.contextmanager.domain.common.IncidentProgress;
import com.simulator112.contextmanager.domain.common.LevelProgress;
import com.simulator112.contextmanager.domain.system112.System112Progress;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.dds.DdsStageSignal;
import com.simulator112.contextmanager.domain.dds.DdsStageType;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.ReactionStatus;
import com.simulator112.contextmanager.domain.common.ReactionStatusEvent;
import com.simulator112.contextmanager.domain.common.ServiceReaction;
import com.simulator112.contextmanager.domain.common.ServiceReactionProgress;
import com.simulator112.contextmanager.domain.common.StageStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LevelProgressService implements LevelProgressUseCase, ProcessDdsTimeoutsUseCase {
    private final ContextStore contextStore;

    @Override
    @Transactional
    public void processExpiredStages() {
        Instant now = Instant.now();
        contextStore.findWithExpiredStages(IncidentTargetType.DDS, IncidentProgressStatus.ACTIVE,
                        StageStatus.ACTIVE, now)
                .forEach(context -> {
                    refreshExpired(context, now);
                    contextStore.save(context);
                });
    }

    @Override
    @Transactional
    public LevelProgress getProgress(UUID contextId) {
        TrainingContext context = requireContext(contextId);
        if (context.getTargetType() == IncidentTargetType.DDS) {
            Instant now = Instant.now();
            ensureReactionHistory(context, now);
            refreshExpired(context, now);
            context = contextStore.save(context);
        }
        return toProgress(context);
    }

    @Override
    @Transactional
    public LevelProgress applyDdsSignal(UUID contextId, UUID incidentId, DdsStageSignal signal) {
        TrainingContext context = requireDdsContext(contextId);
        Instant now = Instant.now();
        refreshExpired(context, now);
        IncidentSnapshot incident = context.getIncidents().stream()
                .filter(value -> value.getSourceId().equals(incidentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Инцидент не относится к контексту: " + incidentId));
        if (incident.getStatus() != IncidentProgressStatus.ACTIVE) {
            throw new IllegalStateException("Инцидент не активен: " + incidentId);
        }
        StageSnapshot stage = activeStage(incident);
        boolean success = expectedSignal(stage.getDdsStageType()) == signal;
        advance(context, incident, stage, success, false, now);
        return toProgress(contextStore.save(context));
    }

    @Override
    @Transactional
    public LevelProgress applyReactionStatus(UUID contextId, UUID incidentId, String serviceCode,
                                             ReactionStatus status, String comment) {
        TrainingContext context = requireDdsContext(contextId);
        Instant now = Instant.now();
        ensureReactionHistory(context, now);
        IncidentSnapshot incident = context.getIncidents().stream()
                .filter(value -> value.getSourceId().equals(incidentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Инцидент не относится к контексту: " + incidentId));
        if (incident.getStatus() != IncidentProgressStatus.ACTIVE) {
            throw new IllegalStateException("Инцидент не активен: " + incidentId);
        }
        if (!serviceCode.equals(incident.getInitialAssignmentService())) {
            throw new IllegalArgumentException("Служба не относится к DDS-инциденту: " + serviceCode);
        }
        ServiceReaction reaction = requireServiceReaction(incident, serviceCode);
        ReactionStatus current = reaction.currentStatus();
        if (!isAllowedReactionTransition(current, status)) {
            throw new IllegalStateException("Недопустимый переход статуса реагирования: " + current + " -> " + status);
        }
        String normalizedComment = comment == null ? null : comment.trim();
        if ((status == ReactionStatus.NOT_ACCEPTED || status == ReactionStatus.WORK_REFUSED)
                && (normalizedComment == null || normalizedComment.isEmpty())) {
            throw new IllegalArgumentException("Для отказа необходимо указать комментарий");
        }
        reaction.getHistory().add(new ReactionStatusEvent(status, now, normalizedComment));
        applyReactionToStage(context, incident, status, now);
        return toProgress(contextStore.save(context));
    }

    private void ensureReactionHistory(TrainingContext context, Instant now) {
        context.getIncidents().stream()
                .filter(incident -> incident.getStatus() == IncidentProgressStatus.ACTIVE)
                .filter(incident -> incident.getServiceReactions().isEmpty())
                .forEach(incident -> {
                    Instant changedAt = activeStage(incident).getStartedAt() == null ? now : activeStage(incident).getStartedAt();
                    ServiceReaction reaction = new ServiceReaction(incident.getInitialAssignmentService());
                    reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ADDED, changedAt, null));
                    reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, changedAt, null));
                    incident.getServiceReactions().add(reaction);
                });
    }

    private ServiceReaction requireServiceReaction(IncidentSnapshot incident, String serviceCode) {
        return incident.getServiceReactions().stream()
                .filter(value -> value.getServiceCode().equals(serviceCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Реагирование службы не найдено: " + serviceCode));
    }

    private boolean isAllowedReactionTransition(ReactionStatus current, ReactionStatus next) {
        return switch (current) {
            case ADDED -> next == ReactionStatus.RECEIVED_BY_SERVICE;
            case RECEIVED_BY_SERVICE -> next == ReactionStatus.ACCEPTED || next == ReactionStatus.NOT_ACCEPTED;
            case NOT_ACCEPTED -> next == ReactionStatus.ACCEPTED;
            case ACCEPTED -> next == ReactionStatus.RESPONSE_STARTED || next == ReactionStatus.WORK_REFUSED;
            case RESPONSE_STARTED -> next == ReactionStatus.ARRIVED || next == ReactionStatus.WORK_REFUSED;
            case ARRIVED -> next == ReactionStatus.WORK_IN_PROGRESS || next == ReactionStatus.WORK_REFUSED;
            case WORK_IN_PROGRESS -> next == ReactionStatus.WORK_COMPLETED || next == ReactionStatus.WORK_REFUSED;
            case WORK_COMPLETED, WORK_REFUSED -> false;
        };
    }

    private void applyReactionToStage(TrainingContext context, IncidentSnapshot incident,
                                      ReactionStatus status, Instant now) {
        if (status == ReactionStatus.NOT_ACCEPTED || status == ReactionStatus.WORK_IN_PROGRESS) {
            return;
        }
        if (status == ReactionStatus.WORK_REFUSED) {
            StageSnapshot stage = activeStage(incident);
            stage.setStatus(StageStatus.FAILED);
            finishIncident(context, incident, false, now);
            return;
        }
        DdsStageType expectedType = switch (status) {
            case ACCEPTED -> DdsStageType.ASSIGN_BRIGADE;
            case RESPONSE_STARTED -> DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE;
            case ARRIVED -> DdsStageType.CALL_BRIGADE_FOR_STATUS;
            case WORK_COMPLETED -> activeStage(incident).getDdsStageType();
            default -> null;
        };
        if (expectedType == null || activeStage(incident).getDdsStageType() != expectedType) {
            throw new IllegalStateException("Статус не соответствует текущему этапу сценария");
        }
        advance(context, incident, activeStage(incident), true, false, now);
    }

    private void refreshExpired(TrainingContext context, Instant now) {
        context.getIncidents().stream()
                .filter(incident -> incident.getStatus() == IncidentProgressStatus.ACTIVE)
                .forEach(incident -> {
                    StageSnapshot stage = activeStage(incident);
                    if (stage.getDeadlineAt() != null && !stage.getDeadlineAt().isAfter(now)) {
                        advance(context, incident, stage, false, true, now);
                    }
                });
    }

    private void advance(TrainingContext context, IncidentSnapshot incident, StageSnapshot stage,
                         boolean success, boolean timedOut, Instant now) {
        stage.setStatus(success ? StageStatus.SUCCEEDED : timedOut ? StageStatus.TIMED_OUT : StageStatus.FAILED);
        UUID nextStageId = incident.getTransitions().stream()
                .filter(value -> value.stageId().equals(stage.getSourceId()))
                .findFirst()
                .map(value -> success ? value.successStageId() : value.failureStageId())
                .orElse(null);
        if (nextStageId == null) {
            finishIncident(context, incident, success, now);
            return;
        }
        activateStage(incident, nextStageId, now);
    }

    private void finishIncident(TrainingContext context, IncidentSnapshot incident, boolean success, Instant now) {
        incident.setActiveStageId(null);
        incident.setStatus(success ? IncidentProgressStatus.COMPLETED : IncidentProgressStatus.FAILED);
        activateNextSequentialIncident(context, now);
        if (context.getIncidents().stream().allMatch(value ->
                value.getStatus() == IncidentProgressStatus.COMPLETED
                        || value.getStatus() == IncidentProgressStatus.FAILED)) {
            context.setStatus(ContextStatus.FILLED);
        }
    }

    private void activateNextSequentialIncident(TrainingContext context, Instant now) {
        if (context.getExecutionMode() != ExecutionMode.SEQUENTIAL) {
            return;
        }
        context.getIncidents().stream()
                .filter(value -> value.getStatus() == IncidentProgressStatus.PENDING)
                .min(Comparator.comparingInt(IncidentSnapshot::getPosition))
                .ifPresent(value -> {
                    value.setStatus(IncidentProgressStatus.ACTIVE);
                    ServiceReaction reaction = new ServiceReaction(value.getInitialAssignmentService());
                    reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ADDED, now, null));
                    reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, now, null));
                    value.getServiceReactions().add(reaction);
                    activateStage(value, value.getInitialStageId(), now);
                });
    }

    private void activateStage(IncidentSnapshot incident, UUID stageId, Instant now) {
        StageSnapshot next = incident.getStages().stream()
                .filter(value -> value.getSourceId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Этап DDS не найден: " + stageId));
        incident.setActiveStageId(stageId);
        next.setStatus(StageStatus.ACTIVE);
        next.setStartedAt(now);
        next.setDeadlineAt(now.plusSeconds(next.getTimeLimitSeconds()));
    }

    private StageSnapshot activeStage(IncidentSnapshot incident) {
        UUID activeStageId = incident.getActiveStageId();
        if (activeStageId == null) {
            throw new IllegalStateException("У активного DDS-инцидента отсутствует активный этап");
        }
        return incident.getStages().stream()
                .filter(value -> value.getSourceId().equals(activeStageId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Активный этап DDS не найден"));
    }

    private DdsStageSignal expectedSignal(DdsStageType type) {
        return switch (type) {
            case ASSIGN_BRIGADE -> DdsStageSignal.BRIGADE_ASSIGNED;
            case WAIT_FOR_BRIGADE_STATUS_CHANGE -> DdsStageSignal.BRIGADE_STATUS_CHANGED;
            case CALL_BRIGADE_FOR_STATUS -> DdsStageSignal.STATUS_CALL_COMPLETED;
            case REQUEST_ADDITIONAL_SERVICE -> DdsStageSignal.ADDITIONAL_SERVICE_REQUESTED;
            case COMPLETE_INCIDENT -> DdsStageSignal.INCIDENT_COMPLETED;
        };
    }

    private TrainingContext requireContext(UUID contextId) {
        return contextStore.findById(contextId)
                .orElseThrow(() -> new IllegalArgumentException("Контекст не найден: " + contextId));
    }

    private TrainingContext requireDdsContext(UUID contextId) {
        TrainingContext context = requireContext(contextId);
        if (context.getTargetType() != IncidentTargetType.DDS) {
            throw new IllegalArgumentException("Контекст не предназначен для DDS");
        }
        return context;
    }

    private LevelProgress toProgress(TrainingContext context) {
        return new LevelProgress(context.getId(), context.getTargetType(), context.getExecutionMode(),
                context.getStatus(), context.getIncidents().stream()
                        .sorted(Comparator.comparingInt(IncidentSnapshot::getPosition))
                        .map(incident -> context.getTargetType() == IncidentTargetType.DDS
                                ? toDdsProgress(incident) : toSystem112Progress(incident))
                        .toList());
    }

    private IncidentProgress toDdsProgress(IncidentSnapshot incident) {
        Instant deadline = incident.getActiveStageId() == null ? null : activeStage(incident).getDeadlineAt();
        return new IncidentProgress(incident.getSourceId(), incident.getStatus(), serviceReactionProgress(incident), null,
                new DdsProgress(incident.getActiveStageId(), deadline,
                        incident.getStages().stream()
                                .map(stage -> new DdsStageProgress(stage.getSourceId(), stage.getDdsStageType(),
                                        stage.getStatus(), stage.getStartedAt(), stage.getDeadlineAt()))
                                .toList()));
    }

    private java.util.List<ServiceReactionProgress> serviceReactionProgress(IncidentSnapshot incident) {
        return incident.getServiceReactions().stream().map(ServiceReactionProgress::from).toList();
    }

    private IncidentProgress toSystem112Progress(IncidentSnapshot incident) {
        var calls = incident.getStages().stream().flatMap(stage -> stage.getCalls().stream()).toList();
        UUID activeCallId = calls.stream()
                .filter(call -> call.getStatus() == CallStatus.ACTIVE || call.getStatus() == CallStatus.DISCONNECTED)
                .map(CallSnapshot::getSourceId)
                .findFirst()
                .orElse(null);
        int completedCalls = (int) calls.stream().filter(call -> call.getStatus() == CallStatus.COMPLETED).count();
        return new IncidentProgress(incident.getSourceId(), incident.getStatus(), serviceReactionProgress(incident),
                new System112Progress(activeCallId, completedCalls, calls.size()), null);
    }
}
