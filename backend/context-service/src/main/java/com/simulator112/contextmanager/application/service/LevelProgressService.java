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
    public LevelProgress applyReactionStatus(UUID contextId, UUID incidentId, String serviceCode,
                                             ReactionStatus status, String comment) {
        TrainingContext context = requireDdsContext(contextId);
        if (context.getStatus() == ContextStatus.IN_REVIEW || context.getStatus() == ContextStatus.DONE) {
            throw new IllegalStateException("Разбор уровня уже начат");
        }
        Instant now = Instant.now();
        ensureReactionHistory(context, now);
        IncidentSnapshot incident = context.getIncidents().stream()
                .filter(value -> value.getSourceId().equals(incidentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Инцидент не относится к контексту: " + incidentId));
        if (incident.getStatus() != IncidentProgressStatus.ACTIVE
                && incident.getStatus() != IncidentProgressStatus.COMPLETED) {
            throw new IllegalStateException("Инцидент ещё не начался: " + incidentId);
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
        if (status == ReactionStatus.ACCEPTED && incident.getStatus() == IncidentProgressStatus.ACTIVE) {
            StageSnapshot stage = activeStage(incident);
            if (stage.getDds().getType() == DdsStageType.ASSIGN_BRIGADE) {
                advance(context, incident, stage, now);
            }
        }
        return toProgress(contextStore.save(context));
    }

    @Override
    @Transactional
    public LevelProgress saveDdsComment(UUID contextId, UUID incidentId, UUID stageId, String comment) {
        TrainingContext context = requireDdsContext(contextId);
        if (context.getStatus() == ContextStatus.IN_REVIEW || context.getStatus() == ContextStatus.DONE) {
            throw new IllegalStateException("Разбор уровня уже начат");
        }
        IncidentSnapshot incident = context.getIncidents().stream()
                .filter(value -> value.getSourceId().equals(incidentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Инцидент не относится к контексту: " + incidentId));
        StageSnapshot stage = incident.getStages().stream()
                .filter(value -> value.getSourceId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Этап не относится к инциденту: " + stageId));
        if (stage.getDds() == null || stage.getDds().getExpectedComment() == null
                || stage.getDds().getExpectedComment().isBlank()) {
            throw new IllegalStateException("На этапе не предусмотрен комментарий");
        }

        if (comment == null || comment.isBlank() || comment.length() > 4000) {
            throw new IllegalArgumentException("Укажите результат звонка (не более 4000 символов)");
        }
        stage.getDds().setComment(comment.trim());
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

    private void refreshExpired(TrainingContext context, Instant now) {
        context.getIncidents().stream()
                .filter(incident -> incident.getStatus() == IncidentProgressStatus.ACTIVE)
                .forEach(incident -> {
                    while (incident.getStatus() == IncidentProgressStatus.ACTIVE) {
                        StageSnapshot stage = activeStage(incident);
                        if (stage.getDeadlineAt() == null || stage.getDeadlineAt().isAfter(now)) break;
                        if (stage.getDds().getType() == DdsStageType.ASSIGN_BRIGADE
                                && incident.getServiceReactions().stream().noneMatch(reaction ->
                                        reaction.getHistory().stream().anyMatch(event -> event.status() == ReactionStatus.ACCEPTED))) {
                            break;
                        }
                        advance(context, incident, stage, stage.getDeadlineAt());
                    }
                });
    }

    private void advance(TrainingContext context, IncidentSnapshot incident, StageSnapshot stage, Instant when) {
        stage.setStatus(StageStatus.SUCCEEDED);
        var stages = incident.getStages().stream().sorted(Comparator.comparingInt(StageSnapshot::getPosition)).toList();
        int index = stages.indexOf(stage);
        if (index < 0 || index + 1 == stages.size()) {
            finishIncident(context, incident, true, when);
            return;
        }
        activateStage(incident, stages.get(index + 1).getSourceId(), when);
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
                    UUID firstStageId = value.getStages().stream()
                            .min(Comparator.comparingInt(StageSnapshot::getPosition))
                            .orElseThrow(() -> new IllegalStateException("Этапы DDS не найдены"))
                            .getSourceId();
                    activateStage(value, firstStageId, now);
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
        next.setDeadlineAt(now.plusSeconds(next.getDds().getTimeLimitSeconds()));
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
                                .map(stage -> new DdsStageProgress(stage.getSourceId(), stage.getDds().getType(),
                                        stage.getStatus(), stage.getStartedAt(), stage.getDeadlineAt(), stage.getDds().getComment(),
                                        stage.getCalls().stream().anyMatch(call -> call.getStatus() == CallStatus.COMPLETED)))
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
