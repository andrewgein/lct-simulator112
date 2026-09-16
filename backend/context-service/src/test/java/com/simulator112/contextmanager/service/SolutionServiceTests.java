package com.simulator112.contextmanager.service;

import com.simulator112.contextmanager.dto.request.PersonInfoRequest;
import com.simulator112.contextmanager.dto.request.SolutionContextRequest;
import com.simulator112.contextmanager.grpc.mapper.SolutionContextMapper;
import com.simulator112.contextmanager.model.embeddable.PersonInfo;
import com.simulator112.contextmanager.model.entity.Context;
import com.simulator112.contextmanager.model.entity.DialupContextEntity;
import com.simulator112.contextmanager.model.entity.IncidentContextEntity;
import com.simulator112.contextmanager.model.entity.SolutionContextEntity;
import com.simulator112.contextmanager.model.entity.StageContextEntity;
import com.simulator112.contextmanager.model.enums.ContextStatus;
import com.simulator112.contextmanager.model.enums.DialogProgressStatus;
import com.simulator112.contextmanager.model.enums.SolutionContextOperation;
import com.simulator112.contextmanager.model.enums.SolutionContextStatus;
import com.simulator112.contextmanager.repository.ContextRepository;
import com.simulator112.contextmanager.repository.SolutionContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolutionServiceTests {
    @Mock
    private ContextRepository contextRepository;

    @Mock
    private SolutionContextRepository solutionContextRepository;

    @InjectMocks
    private SolutionService service;

    private Context context;
    private UUID activeDialupId;

    @BeforeEach
    void setUp() {
        activeDialupId = UUID.randomUUID();
        context = new Context();
        context.setUuid(UUID.randomUUID());
        context.setStatus(ContextStatus.CREATED);
        context.setDialogStatus(DialogProgressStatus.IN_CALL);
        context.setActiveDialupId(activeDialupId);
        when(contextRepository.findById(context.getUuid())).thenReturn(Optional.of(context));
    }

    @Test
    void linksExistingCardWithoutCreatingAnotherCard() {
        allowSave();
        SolutionContextEntity card = card(activeDialupId, SolutionContextStatus.ACTIVE);
        SolutionContextEntity parent = card(UUID.randomUUID(), SolutionContextStatus.ACTIVE);
        stubCard(card);
        stubCard(parent);

        service.saveCardRevision(context.getUuid(), card.getCardId(),
                request(SolutionContextOperation.CREATE_CHILD, card, parent.getCardId()));

        assertEquals(1, context.getSolutionContexts().size());
        SolutionContextEntity revision = context.getSolutionContexts().get(0);
        assertEquals(card.getCardId(), revision.getCardId());
        assertEquals(parent.getCardId(), revision.getParentCardId());
        assertNull(revision.getDuplicateOfCardId());
        assertEquals(2, revision.getVersion());
    }

    @Test
    void marksExistingCardAsDuplicateWithoutCreatingThirdCard() {
        allowSave();
        SolutionContextEntity card = card(activeDialupId, SolutionContextStatus.ACTIVE);
        SolutionContextEntity canonical = card(UUID.randomUUID(), SolutionContextStatus.ACTIVE);
        stubCard(card);
        stubCard(canonical);

        service.saveCardRevision(context.getUuid(), card.getCardId(),
                request(SolutionContextOperation.DUPLICATE, card, canonical.getCardId()));

        assertEquals(1, context.getSolutionContexts().size());
        SolutionContextEntity revision = context.getSolutionContexts().get(0);
        assertEquals(card.getCardId(), revision.getCardId());
        assertEquals(SolutionContextStatus.CLOSED_DUPLICATE, revision.getStatus());
        assertEquals(canonical.getCardId(), revision.getDuplicateOfCardId());
        assertNull(revision.getParentCardId());
    }

    @Test
    void unlinkPreservesCardData() {
        allowSave();
        SolutionContextEntity card = card(activeDialupId, SolutionContextStatus.ACTIVE);
        card.setParentCardId(UUID.randomUUID());
        card.setApplicant(new PersonInfo("79990000000", null, "Иванов", "Иван", null,
                "Москва", null));
        card.setIncidentType("FIRE");
        card.setAdditionalInfoProvided(true);
        card.getAdditionalInfo().put("floor", "3");
        stubCard(card);

        service.saveCardRevision(context.getUuid(), card.getCardId(),
                request(SolutionContextOperation.UNLINK, card, null));

        SolutionContextEntity revision = context.getSolutionContexts().get(0);
        assertNull(revision.getParentCardId());
        assertNull(revision.getDuplicateOfCardId());
        var assembled = SolutionContextMapper.toAssembledView(List.of(card, revision));
        assertEquals("Иван", assembled.applicant().firstName());
        assertEquals("Иванов", assembled.applicant().lastName());
        assertEquals("FIRE", assembled.incidentType());
        assertEquals(Map.of("floor", "3"), assembled.additionalInfo());
    }

    @Test
    void rejectsRelationshipChangesAfterDialupEnds() {
        context.setDialogStatus(DialogProgressStatus.COMPLETED);
        SolutionContextEntity card = card(activeDialupId, SolutionContextStatus.ACTIVE);
        card.setParentCardId(UUID.randomUUID());
        stubCard(card);

        assertThrows(IllegalStateException.class, () -> service.saveCardRevision(
                context.getUuid(), card.getCardId(),
                request(SolutionContextOperation.UNLINK, card, null)));

        verify(solutionContextRepository, never()).saveAndFlush(any());
    }

    @Test
    void creatingDuplicateDoesNotReviseCanonicalCard() {
        allowSave();
        attachDialup(activeDialupId);
        SolutionContextEntity canonical = card(UUID.randomUUID(), SolutionContextStatus.ACTIVE);
        stubCard(canonical);
        SolutionContextRequest request = request(
                SolutionContextOperation.DUPLICATE, canonical, null);

        var response = service.createCardForDialup(context.getUuid(), activeDialupId, request);

        assertEquals(canonical.getCardId(), response.cardId());
        assertEquals(1, context.getSolutionContexts().size());
        SolutionContextEntity duplicate = context.getSolutionContexts().get(0);
        assertEquals(SolutionContextStatus.CLOSED_DUPLICATE, duplicate.getStatus());
        assertEquals(canonical.getCardId(), duplicate.getDuplicateOfCardId());
        verify(solutionContextRepository, times(1)).saveAndFlush(any());
    }

    private void allowSave() {
        when(solutionContextRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            SolutionContextEntity revision = invocation.getArgument(0);
            if (revision.getId() == null) revision.setId(UUID.randomUUID());
            return revision;
        });
    }

    private SolutionContextRequest request(SolutionContextOperation operation,
                                           SolutionContextEntity card, UUID relationTargetId) {
        PersonInfoRequest applicant = new PersonInfoRequest(
                "79990000000", null, "Петров", "Пётр", null, "Москва", null);
        return new SolutionContextRequest(
                applicant, null, Map.of("floor", "5"), "FIRE",
                card.getCardId(), card.getVersion(), operation, relationTargetId);
    }

    private SolutionContextEntity card(UUID dialupId, SolutionContextStatus status) {
        SolutionContextEntity card = new SolutionContextEntity();
        card.setId(UUID.randomUUID());
        card.setContext(context);
        card.setCardId(UUID.randomUUID());
        card.setVersion(1);
        card.setDialupId(dialupId);
        card.setStatus(status);
        return card;
    }

    private void stubCard(SolutionContextEntity card) {
        when(solutionContextRepository.findTopByContextUuidAndCardIdOrderByVersionDesc(
                context.getUuid(), card.getCardId())).thenReturn(Optional.of(card));
    }

    private void attachDialup(UUID dialupId) {
        DialupContextEntity dialup = new DialupContextEntity();
        dialup.setSourceDialupId(dialupId);
        StageContextEntity stage = new StageContextEntity();
        stage.addDialup(dialup);
        IncidentContextEntity incident = new IncidentContextEntity();
        incident.addStage(stage);
        context.attachIncidentContext(incident);
    }
}
