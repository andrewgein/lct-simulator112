package com.simulator112.contextmanager.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simulator112.contextmanager.dto.request.SolutionContextRequest;
import com.simulator112.contextmanager.dto.response.SolutionContextSaveResponse;
import com.simulator112.contextmanager.dto.response.SolutionContextView;
import com.simulator112.contextmanager.grpc.mapper.SolutionContextMapper;
import com.simulator112.contextmanager.model.entity.Context;
import com.simulator112.contextmanager.model.entity.SolutionContextEntity;
import com.simulator112.contextmanager.model.enums.ContextStatus;
import com.simulator112.contextmanager.model.enums.DialogProgressStatus;
import com.simulator112.contextmanager.model.enums.SolutionContextOperation;
import com.simulator112.contextmanager.model.enums.SolutionContextStatus;
import com.simulator112.contextmanager.repository.ContextRepository;
import com.simulator112.contextmanager.repository.SolutionContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionService {
    private final ContextRepository contextRepository;
    private final SolutionContextRepository solutionContextRepository;

    @Transactional
    public SolutionContextSaveResponse createCardForDialup(UUID contextId, UUID dialupId,
                                                           SolutionContextRequest request) {
        Context context = requireEditableContext(contextId);
        requireDialup(context, dialupId);
        SolutionContextOperation operation = operationOr(request, SolutionContextOperation.CREATE);
        SolutionContextSaveResponse response = switch (operation) {
            case CREATE -> createCard(context, dialupId, request, null);
            case CREATE_CHILD -> createChildCard(context, dialupId, request);
            case DUPLICATE -> createDuplicate(context, dialupId, request);
            case SAVE, UNLINK -> throw new IllegalArgumentException(
                    "Для изменения карточки используйте /cards/{cardId}/revisions");
        };
        log.info("Сохранена ревизия карточки {} для контекста {} и диалапа {}",
                response.cardId(), contextId, dialupId);
        return response;
    }

    @Transactional
    public SolutionContextSaveResponse saveCardRevision(UUID contextId, UUID cardId,
                                                        SolutionContextRequest request) {
        Context context = requireEditableContext(contextId);
        requireMatchingCardId(cardId, request.cardId());
        SolutionContextOperation operation = operationOr(request, SolutionContextOperation.SAVE);
        return switch (operation) {
            case SAVE -> saveCard(context, cardId, request);
            case UNLINK -> unlinkCard(context, cardId, request);
            case CREATE_CHILD, DUPLICATE -> relateCard(context, cardId, request, operation);
            case CREATE -> throw new IllegalArgumentException(
                    "Для новой карточки используйте /dialups/{dialupId}/cards");
        };
    }

    @Transactional(readOnly = true)
    public List<SolutionContextView> getCards(UUID contextId) {
        findContext(contextId);
        var revisionsByCard = new LinkedHashMap<UUID, List<SolutionContextEntity>>();
        solutionContextRepository.findByContextUuidOrderByCreatedAtAsc(contextId)
                .forEach(revision -> revisionsByCard
                        .computeIfAbsent(revision.getCardId(), ignored -> new ArrayList<>())
                        .add(revision));
        return revisionsByCard.values().stream()
                .map(SolutionContextMapper::toAssembledView)
                .toList();
    }

    private SolutionContextSaveResponse saveCard(Context context, UUID cardId,
                                                 SolutionContextRequest request) {
        requireNoRelationTarget(request);
        SolutionContextEntity latest = requireRequestedActiveCard(
                context.getUuid(), cardId, request.expectedVersion());
        return save(context, revisionFrom(latest, latest.getStatus(), latest.getParentCardId(),
                latest.getDuplicateOfCardId(), request));
    }

    private SolutionContextSaveResponse relateCard(Context context, UUID cardId,
                                                   SolutionContextRequest request,
                                                   SolutionContextOperation operation) {
        UUID targetId = requireRelationTarget(request);
        SolutionContextEntity card = requireRequestedActiveCard(
                context.getUuid(), cardId, request.expectedVersion());
        requireIndependentCard(card);
        requireActiveDialupForCard(context, card);
        if (cardId.equals(targetId)) {
            throw new IllegalArgumentException("Карточку нельзя связать с самой собой");
        }
        SolutionContextEntity target = requireIndependentActiveCard(context.getUuid(), targetId);
        boolean duplicate = operation == SolutionContextOperation.DUPLICATE;
        return save(context, revisionFrom(card,
                duplicate ? SolutionContextStatus.CLOSED_DUPLICATE : SolutionContextStatus.ACTIVE,
                duplicate ? null : target.getCardId(), duplicate ? target.getCardId() : null, request));
    }

    private SolutionContextSaveResponse unlinkCard(Context context, UUID cardId,
                                                   SolutionContextRequest request) {
        requireNoRelationTarget(request);
        SolutionContextEntity card = requireRequestedCard(
                context.getUuid(), cardId, request.expectedVersion());
        if (isIndependent(card)) {
            throw new IllegalArgumentException("Карточка не связана и не является дубликатом: " + cardId);
        }
        requireActiveDialupForCard(context, card);
        SolutionContextRequest relationOnly = new SolutionContextRequest(
                null, null, null, null, cardId, card.getVersion(), SolutionContextOperation.UNLINK, null);
        return save(context, revisionFrom(
                card, SolutionContextStatus.ACTIVE, null, null, relationOnly));
    }

    private SolutionContextSaveResponse createChildCard(Context context, UUID dialupId,
                                                        SolutionContextRequest request) {
        UUID parentId = requireRelationTarget(request);
        requireIndependentActiveCard(context.getUuid(), parentId);
        return createCard(context, dialupId, request, parentId);
    }

    private SolutionContextSaveResponse createDuplicate(Context context, UUID dialupId,
                                                        SolutionContextRequest request) {
        requireNoRelationTarget(request);
        SolutionContextEntity canonical = requireRequestedActiveCard(
                context.getUuid(), request.cardId(), request.expectedVersion());
        requireIndependentCard(canonical);
        SolutionContextEntity duplicate = newRevision(context, UUID.randomUUID(), null, 1, dialupId,
                SolutionContextStatus.CLOSED_DUPLICATE, null, canonical.getCardId(), request);
        persist(context, duplicate);
        return new SolutionContextSaveResponse(
                canonical.getId(), canonical.getCardId(), canonical.getVersion(), duplicate.getCardId());
    }

    private SolutionContextSaveResponse createCard(Context context, UUID dialupId,
                                                   SolutionContextRequest request, UUID parentCardId) {
        if (request.cardId() != null || request.expectedVersion() != null) {
            throw new IllegalArgumentException("Для новой карточки cardId и expectedVersion должны отсутствовать");
        }
        if (!Objects.equals(parentCardId, request.parentCardId())) {
            throw new IllegalArgumentException("Некорректный parentCardId для новой карточки");
        }
        return save(context, newRevision(context, UUID.randomUUID(), null, 1, dialupId,
                SolutionContextStatus.ACTIVE, parentCardId, null, request));
    }

    private SolutionContextEntity revisionFrom(SolutionContextEntity previous,
                                               SolutionContextStatus status, UUID parentCardId,
                                               UUID duplicateOfCardId, SolutionContextRequest request) {
        return newRevision(previous.getContext(), previous.getCardId(), previous.getId(),
                previous.getVersion() + 1, previous.getDialupId(), status,
                parentCardId, duplicateOfCardId, request);
    }

    private SolutionContextEntity newRevision(Context context, UUID cardId, UUID previousRevisionId,
                                              long version, UUID dialupId, SolutionContextStatus status,
                                              UUID parentCardId, UUID duplicateOfCardId,
                                              SolutionContextRequest request) {
        SolutionContextEntity revision = new SolutionContextEntity();
        revision.setContext(context);
        revision.setCardId(cardId);
        revision.setPreviousRevisionId(previousRevisionId);
        revision.setVersion(version);
        revision.setDialupId(dialupId);
        revision.setStatus(status);
        revision.setParentCardId(parentCardId);
        revision.setDuplicateOfCardId(duplicateOfCardId);
        SolutionContextMapper.fillSnapshot(revision, request);
        return revision;
    }

    private SolutionContextSaveResponse save(Context context, SolutionContextEntity revision) {
        persist(context, revision);
        return new SolutionContextSaveResponse(
                revision.getId(), revision.getCardId(), revision.getVersion(), null);
    }

    private void persist(Context context, SolutionContextEntity revision) {
        context.getSolutionContexts().add(revision);
        solutionContextRepository.saveAndFlush(revision);
    }

    private SolutionContextEntity requireRequestedActiveCard(UUID contextId, UUID cardId,
                                                              Long expectedVersion) {
        SolutionContextEntity card = requireRequestedCard(contextId, cardId, expectedVersion);
        if (card.getStatus() != SolutionContextStatus.ACTIVE) {
            throw new IllegalArgumentException("Карточка закрыта: " + cardId);
        }
        return card;
    }

    private SolutionContextEntity requireRequestedCard(UUID contextId, UUID cardId,
                                                        Long expectedVersion) {
        if (cardId == null || expectedVersion == null) {
            throw new IllegalArgumentException("Для сохранения существующей карточки нужны cardId и expectedVersion");
        }
        SolutionContextEntity card = requireCard(contextId, cardId);
        if (card.getVersion() != expectedVersion) {
            throw new IllegalStateException("Карточка уже изменена: ожидаемая версия "
                    + expectedVersion + ", текущая версия " + card.getVersion());
        }
        return card;
    }

    private SolutionContextEntity requireIndependentActiveCard(UUID contextId, UUID cardId) {
        SolutionContextEntity card = requireCard(contextId, cardId);
        if (card.getStatus() != SolutionContextStatus.ACTIVE) {
            throw new IllegalArgumentException("Карточка закрыта: " + cardId);
        }
        requireIndependentCard(card);
        return card;
    }

    private SolutionContextEntity requireCard(UUID contextId, UUID cardId) {
        return solutionContextRepository
                .findTopByContextUuidAndCardIdOrderByVersionDesc(contextId, cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карточка не найдена: " + cardId));
    }

    private void requireIndependentCard(SolutionContextEntity card) {
        if (!isIndependent(card)) {
            throw new IllegalArgumentException(
                    "Связанную карточку нельзя повторно связать или дублировать: " + card.getCardId());
        }
    }

    private boolean isIndependent(SolutionContextEntity card) {
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

    private void requireActiveDialupForCard(Context context, SolutionContextEntity card) {
        boolean dialupActive = card.getDialupId().equals(context.getActiveDialupId())
                && (context.getDialogStatus() == DialogProgressStatus.IN_CALL
                    || context.getDialogStatus() == DialogProgressStatus.DISCONNECTED);
        if (!dialupActive) {
            throw new IllegalStateException("Связь можно изменить только до завершения диалапа карточки");
        }
    }

    private void requireDialup(Context context, UUID dialupId) {
        boolean belongs = context.getIncidentContexts().stream()
                .flatMap(incident -> incident.getStages().stream())
                .flatMap(stage -> stage.getDialups().stream())
                .anyMatch(dialup -> dialup.getSourceDialupId().equals(dialupId));
        if (!belongs) {
            throw new IllegalArgumentException(
                    "Диалап " + dialupId + " не относится к контексту " + context.getUuid());
        }
    }

    private Context requireEditableContext(UUID contextId) {
        Context context = findContext(contextId);
        if (context.getStatus() == ContextStatus.IN_REVIEW || context.getStatus() == ContextStatus.DONE) {
            throw new IllegalStateException("Контекст уже закрыт для изменений");
        }
        return context;
    }

    private Context findContext(UUID contextId) {
        return contextRepository.findById(contextId)
                .orElseThrow(() -> new IllegalArgumentException("Контекста не существует: " + contextId));
    }

    private SolutionContextOperation operationOr(SolutionContextRequest request,
                                                 SolutionContextOperation fallback) {
        return request.operation() == null ? fallback : request.operation();
    }
}
