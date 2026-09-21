package com.simulator112.contextmanager.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simulator112.contextmanager.application.model.system112.SolutionContextRequest;
import com.simulator112.contextmanager.application.model.system112.SolutionContextSaveResponse;
import com.simulator112.contextmanager.application.model.system112.SolutionContextView;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.application.port.in.ManageSystem112CardUseCase;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.application.port.out.SolutionCardStore;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import com.simulator112.contextmanager.domain.system112.PersonInfo;
import com.simulator112.contextmanager.domain.common.DialogProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.system112.SolutionContextOperation;
import com.simulator112.contextmanager.domain.system112.SolutionContextStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionService implements ManageSystem112CardUseCase {
    private final ContextStore contextStore;
    private final SolutionCardStore solutionCardStore;

    @Transactional
    public SolutionContextSaveResponse createCardForCall(UUID contextId, UUID callId,
                                                           SolutionContextRequest request) {
        TrainingContext context = requireEditableContext(contextId);
        requireCall(context, callId);
        SolutionContextOperation operation = operationOr(request, SolutionContextOperation.CREATE);
        SolutionContextSaveResponse response = switch (operation) {
            case CREATE -> createCard(context, callId, request, null);
            case CREATE_CHILD -> createChildCard(context, callId, request);
            case DUPLICATE -> createDuplicate(context, callId, request);
            case SAVE, UNLINK -> throw new IllegalArgumentException(
                    "Для изменения карточки используйте /cards/{cardId}/revisions");
        };
        log.info("Сохранена ревизия карточки {} для контекста {} и звонка {}",
                response.cardId(), contextId, callId);
        return response;
    }

    @Transactional
    public SolutionContextSaveResponse saveCardRevision(UUID contextId, UUID cardId,
                                                        SolutionContextRequest request) {
        TrainingContext context = requireEditableContext(contextId);
        requireMatchingCardId(cardId, request.cardId());
        SolutionContextOperation operation = operationOr(request, SolutionContextOperation.SAVE);
        return switch (operation) {
            case SAVE -> saveCard(context, cardId, request);
            case UNLINK -> unlinkCard(context, cardId, request);
            case CREATE_CHILD, DUPLICATE -> relateCard(context, cardId, request, operation);
            case CREATE -> throw new IllegalArgumentException(
                    "Для новой карточки используйте /calls/{callId}/cards");
        };
    }

    @Transactional(readOnly = true)
    public List<SolutionContextView> getCards(UUID contextId) {
        requireSystem112Context(contextId);
        var revisionsByCard = new LinkedHashMap<UUID, List<SolutionCardRevision>>();
        solutionCardStore.findAll(contextId)
                .forEach(revision -> revisionsByCard
                        .computeIfAbsent(revision.getCardId(), ignored -> new ArrayList<>())
                        .add(revision));
        return revisionsByCard.values().stream()
                .map(this::toAssembledView)
                .toList();
    }

    private SolutionContextSaveResponse saveCard(TrainingContext context, UUID cardId,
                                                 SolutionContextRequest request) {
        requireNoRelationTarget(request);
        SolutionCardRevision latest = requireRequestedActiveCard(
                context.getId(), cardId, request.expectedVersion());
        return save(context, revisionFrom(context, latest, latest.getStatus(), latest.getParentCardId(),
                latest.getDuplicateOfCardId(), request));
    }

    private SolutionContextSaveResponse relateCard(TrainingContext context, UUID cardId,
                                                   SolutionContextRequest request,
                                                   SolutionContextOperation operation) {
        UUID targetId = requireRelationTarget(request);
        SolutionCardRevision card = requireRequestedActiveCard(
                context.getId(), cardId, request.expectedVersion());
        requireIndependentCard(card);
        requireActiveCallForCard(context, card);
        if (cardId.equals(targetId)) {
            throw new IllegalArgumentException("Карточку нельзя связать с самой собой");
        }
        SolutionCardRevision target = requireIndependentActiveCard(context.getId(), targetId);
        boolean duplicate = operation == SolutionContextOperation.DUPLICATE;
        return save(context, revisionFrom(context, card,
                duplicate ? SolutionContextStatus.CLOSED_DUPLICATE : SolutionContextStatus.ACTIVE,
                duplicate ? null : target.getCardId(), duplicate ? target.getCardId() : null, request));
    }

    private SolutionContextSaveResponse unlinkCard(TrainingContext context, UUID cardId,
                                                   SolutionContextRequest request) {
        requireNoRelationTarget(request);
        SolutionCardRevision card = requireRequestedCard(
                context.getId(), cardId, request.expectedVersion());
        if (isIndependent(card)) {
            throw new IllegalArgumentException("Карточка не связана и не является дубликатом: " + cardId);
        }
        requireActiveCallForCard(context, card);
        SolutionContextRequest relationOnly = new SolutionContextRequest(
                null, null, null, null, cardId, card.getVersion(), SolutionContextOperation.UNLINK, null);
        return save(context, revisionFrom(
                context, card, SolutionContextStatus.ACTIVE, null, null, relationOnly));
    }

    private SolutionContextSaveResponse createChildCard(TrainingContext context, UUID callId,
                                                        SolutionContextRequest request) {
        UUID parentId = requireRelationTarget(request);
        requireIndependentActiveCard(context.getId(), parentId);
        return createCard(context, callId, request, parentId);
    }

    private SolutionContextSaveResponse createDuplicate(TrainingContext context, UUID callId,
                                                        SolutionContextRequest request) {
        requireNoRelationTarget(request);
        SolutionCardRevision canonical = requireRequestedActiveCard(
                context.getId(), request.cardId(), request.expectedVersion());
        requireIndependentCard(canonical);
        SolutionCardRevision duplicate = newRevision(context, UUID.randomUUID(), null, 1, callId,
                SolutionContextStatus.CLOSED_DUPLICATE, null, canonical.getCardId(), request);
        SolutionCardRevision savedDuplicate = persist(context, duplicate);
        return new SolutionContextSaveResponse(
                canonical.getId(), canonical.getCardId(), canonical.getVersion(), savedDuplicate.getCardId());
    }

    private SolutionContextSaveResponse createCard(TrainingContext context, UUID callId,
                                                   SolutionContextRequest request, UUID parentCardId) {
        if (request.cardId() != null || request.expectedVersion() != null) {
            throw new IllegalArgumentException("Для новой карточки cardId и expectedVersion должны отсутствовать");
        }
        if (!Objects.equals(parentCardId, request.parentCardId())) {
            throw new IllegalArgumentException("Некорректный parentCardId для новой карточки");
        }
        return save(context, newRevision(context, UUID.randomUUID(), null, 1, callId,
                SolutionContextStatus.ACTIVE, parentCardId, null, request));
    }

    private SolutionCardRevision revisionFrom(TrainingContext context, SolutionCardRevision previous,
                                               SolutionContextStatus status, UUID parentCardId,
                                               UUID duplicateOfCardId, SolutionContextRequest request) {
        return newRevision(context, previous.getCardId(), previous.getId(),
                previous.getVersion() + 1, previous.getCallId(), status,
                parentCardId, duplicateOfCardId, request);
    }

    private SolutionCardRevision newRevision(TrainingContext context, UUID cardId, UUID previousRevisionId,
                                              long version, UUID callId, SolutionContextStatus status,
                                              UUID parentCardId, UUID duplicateOfCardId,
                                              SolutionContextRequest request) {
        SolutionCardRevision revision = new SolutionCardRevision();
        revision.setContextId(context.getId());
        revision.setCardId(cardId);
        revision.setPreviousRevisionId(previousRevisionId);
        revision.setVersion(version);
        revision.setCallId(callId);
        revision.setStatus(status);
        revision.setParentCardId(parentCardId);
        revision.setDuplicateOfCardId(duplicateOfCardId);
        fillSnapshot(revision, request);
        return revision;
    }

    private SolutionContextSaveResponse save(TrainingContext context, SolutionCardRevision revision) {
        SolutionCardRevision saved = persist(context, revision);
        return new SolutionContextSaveResponse(
                saved.getId(), saved.getCardId(), saved.getVersion(), null);
    }

    private SolutionCardRevision persist(TrainingContext context, SolutionCardRevision revision) {
        SolutionCardRevision saved = solutionCardStore.save(revision);
        context.getSolutionCards().add(saved);
        return saved;
    }

    private SolutionCardRevision requireRequestedActiveCard(UUID contextId, UUID cardId,
                                                              Long expectedVersion) {
        SolutionCardRevision card = requireRequestedCard(contextId, cardId, expectedVersion);
        if (card.getStatus() != SolutionContextStatus.ACTIVE) {
            throw new IllegalArgumentException("Карточка закрыта: " + cardId);
        }
        return card;
    }

    private SolutionCardRevision requireRequestedCard(UUID contextId, UUID cardId,
                                                        Long expectedVersion) {
        if (cardId == null || expectedVersion == null) {
            throw new IllegalArgumentException("Для сохранения существующей карточки нужны cardId и expectedVersion");
        }
        SolutionCardRevision card = requireCard(contextId, cardId);
        if (card.getVersion() != expectedVersion) {
            throw new IllegalStateException("Карточка уже изменена: ожидаемая версия "
                    + expectedVersion + ", текущая версия " + card.getVersion());
        }
        return card;
    }

    private SolutionCardRevision requireIndependentActiveCard(UUID contextId, UUID cardId) {
        SolutionCardRevision card = requireCard(contextId, cardId);
        if (card.getStatus() != SolutionContextStatus.ACTIVE) {
            throw new IllegalArgumentException("Карточка закрыта: " + cardId);
        }
        requireIndependentCard(card);
        return card;
    }

    private SolutionCardRevision requireCard(UUID contextId, UUID cardId) {
        return solutionCardStore.findLatestByCard(contextId, cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карточка не найдена: " + cardId));
    }

    private void requireIndependentCard(SolutionCardRevision card) {
        if (!isIndependent(card)) {
            throw new IllegalArgumentException(
                    "Связанную карточку нельзя повторно связать или дублировать: " + card.getCardId());
        }
    }

    private boolean isIndependent(SolutionCardRevision card) {
        return card.getParentCardId() == null && card.getDuplicateOfCardId() == null;
    }

    private UUID requireRelationTarget(SolutionContextRequest request) {
        if (request.parentCardId() == null) {
            throw new IllegalArgumentException("Для связи требуется идентификатор целевой карточки");
        }
        return request.parentCardId();
    }

    private void requireNoRelationTarget(SolutionContextRequest request) {
        if (request.parentCardId() != null) {
            throw new IllegalArgumentException("parentCardId допустим только для операции связи");
        }
    }

    private void requireMatchingCardId(UUID cardId, UUID requestedCardId) {
        if (requestedCardId != null && !requestedCardId.equals(cardId)) {
            throw new IllegalArgumentException("cardId в пути и теле запроса не совпадают");
        }
    }

    private void requireActiveCallForCard(TrainingContext context, SolutionCardRevision card) {
        boolean callActive = card.getCallId().equals(context.getActiveCallId())
                && (context.getDialogStatus() == DialogProgressStatus.IN_CALL
                    || context.getDialogStatus() == DialogProgressStatus.DISCONNECTED);
        if (!callActive) {
            throw new IllegalStateException("Связь можно изменить только до завершения звонка карточки");
        }
    }

    private void requireCall(TrainingContext context, UUID callId) {
        boolean belongs = context.getIncidents().stream()
                .flatMap(incident -> incident.getStages().stream())
                .flatMap(stage -> stage.getCalls().stream())
                .anyMatch(call -> call.getSourceId().equals(callId));
        if (!belongs) {
            throw new IllegalArgumentException(
                    "Звонок " + callId + " не относится к контексту " + context.getId());
        }
    }

    private TrainingContext requireEditableContext(UUID contextId) {
        TrainingContext context = requireSystem112Context(contextId);
        if (context.getStatus() == ContextStatus.IN_REVIEW || context.getStatus() == ContextStatus.DONE) {
            throw new IllegalStateException("Контекст уже закрыт для изменений");
        }
        return context;
    }

    private TrainingContext requireSystem112Context(UUID contextId) {
        TrainingContext context = findContext(contextId);
        if (context.getTargetType() != IncidentTargetType.SYSTEM_112) {
            throw new IllegalArgumentException("Операции с карточками доступны только оператору системы 112");
        }
        return context;
    }

    private TrainingContext findContext(UUID contextId) {
        return contextStore.findById(contextId)
                .orElseThrow(() -> new IllegalArgumentException("Контекста не существует: " + contextId));
    }

    private void fillSnapshot(SolutionCardRevision revision, SolutionContextRequest request) {
        revision.setApplicant(toPerson(request.applicant()));
        revision.setVictim(toPerson(request.victim()));
        revision.setAdditionalInfoProvided(request.additionalInfo() != null);
        if (request.additionalInfo() != null) revision.getAdditionalInfo().putAll(request.additionalInfo());
        revision.setIncidentType(request.incidentType());
    }

    private PersonInfo toPerson(com.simulator112.contextmanager.application.model.system112.PersonInfoRequest value) {
        return value == null ? null : new PersonInfo(value.phone(), value.contactPhone(), value.lastName(),
                value.firstName(), value.middleName(), value.address(), value.additionalInfo());
    }

    private SolutionContextView toAssembledView(List<SolutionCardRevision> revisions) {
        var ordered = revisions.stream().sorted(java.util.Comparator.comparingLong(SolutionCardRevision::getVersion)).toList();
        com.simulator112.contextmanager.application.model.system112.PersonInfoRequest applicant = null;
        com.simulator112.contextmanager.application.model.system112.PersonInfoRequest victim = null;
        Map<String, String> additionalInfo = Map.of();
        String incidentType = null;
        for (SolutionCardRevision revision : ordered) {
            applicant = merge(applicant, revision.getApplicant()); victim = merge(victim, revision.getVictim());
            if (revision.isAdditionalInfoProvided()) additionalInfo = Map.copyOf(revision.getAdditionalInfo());
            if (revision.getIncidentType() != null) incidentType = revision.getIncidentType();
        }
        var latest = ordered.getLast();
        return new SolutionContextView(latest.getId(), latest.getCardId(), latest.getPreviousRevisionId(), latest.getVersion(),
                latest.getStatus(), latest.getCallId(), latest.getParentCardId(), latest.getDuplicateOfCardId(),
                applicant, victim, additionalInfo, incidentType, latest.getCreatedAt());
    }

    private com.simulator112.contextmanager.application.model.system112.PersonInfoRequest merge(
            com.simulator112.contextmanager.application.model.system112.PersonInfoRequest previous, PersonInfo next) {
        if (next == null) return previous;
        return new com.simulator112.contextmanager.application.model.system112.PersonInfoRequest(
                next.phone() == null && previous != null ? previous.phone() : next.phone(),
                next.contactPhone() == null && previous != null ? previous.contactPhone() : next.contactPhone(),
                next.lastName() == null && previous != null ? previous.lastName() : next.lastName(),
                next.firstName() == null && previous != null ? previous.firstName() : next.firstName(),
                next.middleName() == null && previous != null ? previous.middleName() : next.middleName(),
                next.address() == null && previous != null ? previous.address() : next.address(),
                next.additionalInfo() == null && previous != null ? previous.additionalInfo() : next.additionalInfo());
    }

    private SolutionContextOperation operationOr(SolutionContextRequest request,
                                                 SolutionContextOperation fallback) {
        return request.operation() == null ? fallback : request.operation();
    }
}
