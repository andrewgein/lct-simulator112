package com.simulator112.contextmanager.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.contextmanager.grpc.client.IncidentServiceGrpcClient;
import com.simulator112.contextmanager.grpc.client.ReviewServiceGrpcClient;
import com.simulator112.contextmanager.grpc.mapper.FullContextMapper;
import com.simulator112.contextmanager.grpc.mapper.IncidentContextMapper;
import com.simulator112.contextmanager.model.entity.Context;
import com.simulator112.contextmanager.model.enums.ContextStatus;
import com.simulator112.contextmanager.model.enums.DialogProgressStatus;
import com.simulator112.contextmanager.repository.ContextRepository;
import com.simulator112.review.grpc.contract.SendOnReviewRequest;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.LevelContext;
import com.simulator112.contextmanager.model.entity.DialupContextEntity;
import com.simulator112.contextmanager.model.entity.IncidentContextEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContextService {
    private final ContextRepository contextRepository;
    private final ReviewServiceGrpcClient reviewService;
    private final IncidentServiceGrpcClient incidentService;

    @Transactional(readOnly = true)
    public IncidentContext getIncidentContext(String id) {
        Context context = find(id);
        if (context.getIncidentContexts().isEmpty()) {
            throw new IllegalStateException("IncidentContext отсутствует для " + id);
        }
        UUID activeDialupId = context.getActiveDialupId();
        IncidentContextEntity incident = activeDialupId == null
                ? incidentForFirstQueuedDialup(context)
                : incidentForDialup(context, activeDialupId);
        return IncidentContextMapper.toProto(incident);
    }

    @Transactional
    public void closeContext(UUID id) {
        Context context = find(id);
        if (!isComplete(context)) {
            throw new IllegalStateException("Контекст " + id + " нельзя закрыть: не хватает данных "
                    + "(инцидент/диалог/хотя бы одна карточка решения)");
        }
        sendOnReview(context);
    }

    @Transactional
    public Context create(UUID userId, UUID levelId) {
        Context context = new Context();
        context.setLevelId(levelId);
        context.setUserId(userId);
        context.setStatus(ContextStatus.CREATED);
        context.setDialogStatus(DialogProgressStatus.IDLE);

        LevelContext level = incidentService.getLevelContext(levelId);
        context.setDifficulty(com.simulator112.shared.dto.Difficulty.valueOf(level.getDifficulty().name()));
        if (level.getIncidentsCount() == 0) {
            throw new IllegalStateException("На уровне " + levelId + " нет инцидентов");
        }
        level.getIncidentsList().stream()
                .map(IncidentContextMapper::toEntity)
                .forEach(context::attachIncidentContext);
        assignRandomizedQueue(context.getIncidentContexts());
        return contextRepository.save(context);
    }

    private void assignRandomizedQueue(List<IncidentContextEntity> incidents) {
        List<ArrayDeque<DialupContextEntity>> queues = incidents.stream()
                .map(incident -> new ArrayDeque<>(incident.getStages().stream()
                        .sorted(Comparator.comparing(stage -> stage.getPosition()))
                        .flatMap(stage -> stage.getDialups().stream()
                                .sorted(Comparator.comparing(DialupContextEntity::getPosition)))
                        .toList()))
                .filter(queue -> !queue.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        int position = 0;
        while (!queues.isEmpty()) {
            int queueIndex = ThreadLocalRandom.current().nextInt(queues.size());
            ArrayDeque<DialupContextEntity> queue = queues.get(queueIndex);
            queue.removeFirst().setQueuePosition(position++);
            if (queue.isEmpty()) {
                queues.remove(queueIndex);
            }
        }
    }

    private IncidentContextEntity incidentForFirstQueuedDialup(Context context) {
        return context.getIncidentContexts().stream()
                .filter(incident -> incident.getStages().stream()
                        .flatMap(stage -> stage.getDialups().stream())
                        .findAny().isPresent())
                .min(Comparator.comparing(incident -> incident.getStages().stream()
                        .flatMap(stage -> stage.getDialups().stream())
                        .map(DialupContextEntity::getQueuePosition)
                        .min(Integer::compareTo)
                        .orElse(Integer.MAX_VALUE)))
                .orElse(context.getIncidentContexts().get(0));
    }

    private IncidentContextEntity incidentForDialup(Context context, UUID dialupId) {
        return context.getIncidentContexts().stream()
                .filter(incident -> incident.getStages().stream()
                        .flatMap(stage -> stage.getDialups().stream())
                        .anyMatch(dialup -> dialup.getSourceDialupId().equals(dialupId)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Диалап " + dialupId + " не относится к контексту " + context.getUuid()));
    }

    private boolean isComplete(Context context) {
        return context.getUuid() != null
                && !context.getIncidentContexts().isEmpty()
                && context.getDialogContext() != null
                && !context.getSolutionContexts().isEmpty();
    }

    private void sendOnReview(Context context) {
        context.setStatus(ContextStatus.IN_REVIEW);
        context = contextRepository.save(context);
        FullContext fullContext = FullContextMapper.toProto(context);
        try {
            reviewService.sendFullContext(SendOnReviewRequest.newBuilder().setContext(fullContext).build());
            log.info("Контекст {} отправлен на ревью", context.getUuid());
        } catch (Exception e) {
            log.error("Не удалось получить ревью, причина: {}", e.getMessage());
        }
    }

    private Context find(String id) {
        return contextRepository.findById(parseUuid(id))
                .orElseThrow(() -> new IllegalArgumentException("Контекста не существует: " + id));
    }

    private Context find(UUID id) {
        return contextRepository.findById(id)
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
