package com.simulator112.contextmanager.application.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simulator112.contextmanager.application.port.in.CleanupAbandonedContextsUseCase;
import com.simulator112.contextmanager.application.port.in.ContextUseCase;
import com.simulator112.contextmanager.application.port.out.CourseAssignmentPort;
import com.simulator112.contextmanager.application.port.out.ReviewPort;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.DialogProgressStatus;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.ReactionStatus;
import com.simulator112.contextmanager.domain.common.ReactionStatusEvent;
import com.simulator112.contextmanager.domain.common.ServiceReaction;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextService implements ContextUseCase, CleanupAbandonedContextsUseCase {
    private final ContextStore contextStore;
    private final ReviewPort reviewService;
    private final CourseAssignmentPort courseAssignments;

    @Override
    @Transactional(readOnly = true)
    public IncidentSnapshot getIncidentContext(String id) {
        TrainingContext context = find(id);
        if (context.getIncidents().isEmpty()) {
            throw new IllegalStateException("IncidentContext отсутствует для " + id);
        }
        UUID activeCallId = context.getActiveCallId();
        IncidentSnapshot incident;
        if (activeCallId != null) {
            incident = incidentForCall(context, activeCallId);
        } else if (context.getTargetType() == IncidentTargetType.DDS) {
            incident = context.getIncidents().stream()
                    .filter(value -> value.getStatus() == IncidentProgressStatus.ACTIVE)
                    .findFirst()
                    .orElse(context.getIncidents().getFirst());
        } else {
            incident = incidentForFirstQueuedCall(context);
        }
        return incident;
    }

    @Override
    @Transactional
    public void closeContext(UUID id) {
        TrainingContext context = find(id);
        if (context.getStatus() == ContextStatus.IN_REVIEW || context.getStatus() == ContextStatus.DONE) return;
        if (!isComplete(context)) {
            throw new IllegalStateException("Контекст " + id + " нельзя закрыть: не хватает данных "
                    + "(инцидент/диалог/хотя бы одна карточка решения)");
        }
        sendOnReview(context);
    }

    @Override
    @Transactional
    public UUID createContext(UUID userId, UUID assignmentId) {
        return create(userId, assignmentId).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findActiveContext(UUID userId, UUID assignmentId) {
        return contextStore.findActive(userId, assignmentId).map(TrainingContext::getId);
    }

    @Override
    @Transactional
    public void cleanupAbandoned(Instant updatedBefore) {
        List<TrainingContext> abandoned = contextStore.findAbandoned(updatedBefore);
        abandoned.forEach(context -> contextStore.delete(context.getId()));
        if (!abandoned.isEmpty()) {
            log.info("Удалено {} брошенных контекстов без активности с {}", abandoned.size(), updatedBefore);
        }
    }

    @Transactional
    public TrainingContext create(UUID userId, UUID assignmentId) {
        var assignment = courseAssignments.getAssignmentForUser(assignmentId, userId);
        TrainingContext context = new TrainingContext();
        context.setAssignmentId(assignmentId);
        context.setLevelTitle(assignment.title());
        context.setThreshold3(assignment.threshold3());
        context.setThreshold4(assignment.threshold4());
        context.setThreshold5(assignment.threshold5());
        context.setUserId(userId);
        context.setStatus(ContextStatus.CREATED);
        context.setDialogStatus(DialogProgressStatus.IDLE);
        context.setDifficulty(assignment.difficulty());
        context.setTargetType(assignment.targetType());
        context.setExecutionMode(assignment.executionMode());
        context.getIncidents().addAll(assignment.incidents());
        initializeProgress(context);
        return contextStore.save(context);
    }

    private void initializeProgress(TrainingContext context) {
        if (context.getTargetType() == IncidentTargetType.SYSTEM_112) {
            if (context.getExecutionMode() == ExecutionMode.PARALLEL) {
                context.getIncidents().forEach(incident -> incident.setStatus(IncidentProgressStatus.ACTIVE));
                assignRandomizedQueue(context.getIncidents());
            } else {
                assignSequentialQueue(context.getIncidents());
                context.getIncidents().getFirst().setStatus(IncidentProgressStatus.ACTIVE);
            }
            return;
        }
        if (context.getExecutionMode() == ExecutionMode.PARALLEL) {
            context.getIncidents().forEach(this::activateDdsIncident);
        } else {
            activateDdsIncident(context.getIncidents().getFirst());
        }
    }

    private void activateDdsIncident(IncidentSnapshot incident) {
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        var stage = incident.getStages().stream()
                .min(Comparator.comparingInt(StageSnapshot::getPosition))
                .orElseThrow(() -> new IllegalStateException("Этапы DDS не найдены"));
        incident.setActiveStageId(stage.getSourceId());
        var now = java.time.Instant.now();
        ServiceReaction reaction = new ServiceReaction(incident.getInitialAssignmentService());
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.ADDED, now, null));
        reaction.getHistory().add(new ReactionStatusEvent(ReactionStatus.RECEIVED_BY_SERVICE, now, null));
        incident.getServiceReactions().add(reaction);
        stage.setStatus(StageStatus.ACTIVE);
        stage.setStartedAt(now);
        stage.setDeadlineAt(now.plusSeconds(stage.getDds().getTimeLimitSeconds()));
    }

    private void assignSequentialQueue(List<IncidentSnapshot> incidents) {
        int position = 0;
        for (IncidentSnapshot incident : incidents.stream()
                .sorted(Comparator.comparing(IncidentSnapshot::getPosition)).toList()) {
            for (CallSnapshot call : callsOf(incident)) {
                call.setQueuePosition(position++);
            }
        }
    }

    private List<CallSnapshot> callsOf(IncidentSnapshot incident) {
        return incident.getStages().stream()
                .sorted(Comparator.comparing(stage -> stage.getPosition()))
                .flatMap(stage -> stage.getCalls().stream()
                        .sorted(Comparator.comparing(CallSnapshot::getPosition)))
                .toList();
    }

    private void assignRandomizedQueue(List<IncidentSnapshot> incidents) {
        List<ArrayDeque<CallSnapshot>> queues = incidents.stream()
                .map(incident -> new ArrayDeque<>(callsOf(incident)))
                .filter(queue -> !queue.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        int position = 0;
        while (!queues.isEmpty()) {
            int queueIndex = ThreadLocalRandom.current().nextInt(queues.size());
            ArrayDeque<CallSnapshot> queue = queues.get(queueIndex);
            queue.removeFirst().setQueuePosition(position++);
            if (queue.isEmpty()) {
                queues.remove(queueIndex);
            }
        }
    }

    private IncidentSnapshot incidentForFirstQueuedCall(TrainingContext context) {
        return context.getIncidents().stream()
                .filter(incident -> incident.getStages().stream()
                        .flatMap(stage -> stage.getCalls().stream())
                        .findAny().isPresent())
                .min(Comparator.comparing(incident -> incident.getStages().stream()
                        .flatMap(stage -> stage.getCalls().stream())
                        .map(CallSnapshot::getQueuePosition)
                        .filter(java.util.Objects::nonNull)
                        .min(Integer::compareTo)
                        .orElse(Integer.MAX_VALUE)))
                .orElse(context.getIncidents().get(0));
    }

    private IncidentSnapshot incidentForCall(TrainingContext context, UUID callId) {
        return context.getIncidents().stream()
                .filter(incident -> incident.getStages().stream()
                        .flatMap(stage -> stage.getCalls().stream())
                        .anyMatch(call -> call.getSourceId().equals(callId)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Звонок " + callId + " не относится к контексту " + context.getId()));
    }

    private boolean isComplete(TrainingContext context) {
        if (context.getId() == null || context.getIncidents().isEmpty()) {
            return false;
        }
        if (context.getTargetType() == IncidentTargetType.DDS) {
            return context.getDialogStatus() != DialogProgressStatus.IN_CALL
                    && context.getIncidents().stream().allMatch(incident ->
                    incident.getStatus() == IncidentProgressStatus.COMPLETED
                            || incident.getStatus() == IncidentProgressStatus.FAILED);
        }
        return context.getDialog() != null && !context.getSolutionCards().isEmpty()
                && context.getIncidents().stream().allMatch(incident ->
                        incident.getStatus() == IncidentProgressStatus.COMPLETED
                        && incident.getStages().stream().flatMap(stage -> stage.getCalls().stream())
                        .allMatch(call -> call.getStatus() == CallStatus.COMPLETED));
    }

    private void sendOnReview(TrainingContext context) {
        context.setStatus(ContextStatus.IN_REVIEW);
        context = contextStore.save(context);
        try {
            if (reviewService.send(context)) {
                context.setStatus(ContextStatus.DONE);
                context.setDialog(null);
                contextStore.save(context);
            }
            log.info("Контекст {} отправлен на ревью", context.getId());
        } catch (Exception e) {
            log.error("Не удалось получить ревью, причина: {}", e.getMessage());
            throw new IllegalStateException("Не удалось отправить результат на проверку", e);
        }
    }

    private TrainingContext find(String id) {
        return contextStore.findById(parseUuid(id))
                .orElseThrow(() -> new IllegalArgumentException("Контекста не существует: " + id));
    }

    private TrainingContext find(UUID id) {
        return contextStore.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Контекста не существует: " + id));
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Невалидный UUID: " + raw);
        }
    }
}
