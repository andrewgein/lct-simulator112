package com.simulator112.contextmanager.application.service;

import com.simulator112.contextmanager.application.model.system112.PersonInfoRequest;
import com.simulator112.contextmanager.application.model.system112.SolutionContextRequest;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.application.port.out.SolutionCardStore;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import com.simulator112.contextmanager.domain.system112.PersonInfo;
import com.simulator112.contextmanager.domain.common.DialogProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.system112.SolutionContextOperation;
import com.simulator112.contextmanager.domain.system112.SolutionContextStatus;
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
    private ContextStore contextRepository;

    @Mock
    private SolutionCardStore solutionContextRepository;

    @InjectMocks
    private SolutionService service;

    private TrainingContext context;
    private UUID activeCallId;

    @BeforeEach
    void setUp() {
        activeCallId = UUID.randomUUID();
        context = new TrainingContext();
        context.setId(UUID.randomUUID());
        context.setStatus(ContextStatus.CREATED);
        context.setTargetType(IncidentTargetType.SYSTEM_112);
        context.setDialogStatus(DialogProgressStatus.IN_CALL);
        context.setActiveCallId(activeCallId);
        when(contextRepository.findById(context.getId())).thenReturn(Optional.of(context));
    }

    @Test
    void linksExistingCardWithoutCreatingAnotherCard() {
        allowSave();
        SolutionCardRevision card = card(activeCallId, SolutionContextStatus.ACTIVE);
        SolutionCardRevision parent = card(UUID.randomUUID(), SolutionContextStatus.ACTIVE);
        stubCard(card);
        stubCard(parent);

        service.saveCardRevision(context.getId(), card.getCardId(),
                request(SolutionContextOperation.CREATE_CHILD, card, parent.getCardId()));

        assertEquals(1, context.getSolutionCards().size());
        SolutionCardRevision revision = context.getSolutionCards().get(0);
        assertEquals(card.getCardId(), revision.getCardId());
        assertEquals(parent.getCardId(), revision.getParentCardId());
        assertNull(revision.getDuplicateOfCardId());
        assertEquals(2, revision.getVersion());
    }

    @Test
    void marksExistingCardAsDuplicateWithoutCreatingThirdCard() {
        allowSave();
        SolutionCardRevision card = card(activeCallId, SolutionContextStatus.ACTIVE);
        SolutionCardRevision canonical = card(UUID.randomUUID(), SolutionContextStatus.ACTIVE);
        stubCard(card);
        stubCard(canonical);

        service.saveCardRevision(context.getId(), card.getCardId(),
                request(SolutionContextOperation.DUPLICATE, card, canonical.getCardId()));

        assertEquals(1, context.getSolutionCards().size());
        SolutionCardRevision revision = context.getSolutionCards().get(0);
        assertEquals(card.getCardId(), revision.getCardId());
        assertEquals(SolutionContextStatus.CLOSED_DUPLICATE, revision.getStatus());
        assertEquals(canonical.getCardId(), revision.getDuplicateOfCardId());
        assertNull(revision.getParentCardId());
    }

    @Test
    void unlinkPreservesCardData() {
        allowSave();
        SolutionCardRevision card = card(activeCallId, SolutionContextStatus.ACTIVE);
        card.setParentCardId(UUID.randomUUID());
        card.setApplicant(new PersonInfo("79990000000", null, "Иванов", "Иван", null,
                "Москва", null));
        card.setIncidentType("FIRE");
        card.setAdditionalInfoProvided(true);
        card.getAdditionalInfo().put("floor", "3");
        stubCard(card);

        service.saveCardRevision(context.getId(), card.getCardId(),
                request(SolutionContextOperation.UNLINK, card, null));

        SolutionCardRevision revision = context.getSolutionCards().get(0);
        assertNull(revision.getParentCardId());
        assertNull(revision.getDuplicateOfCardId());
        when(solutionContextRepository.findAll(context.getId())).thenReturn(List.of(card, revision));
        var assembled = service.getCards(context.getId()).getFirst();
        assertEquals("Иван", assembled.applicant().firstName());
        assertEquals("Иванов", assembled.applicant().lastName());
        assertEquals("FIRE", assembled.incidentType());
        assertEquals(Map.of("floor", "3"), assembled.additionalInfo());
    }

    @Test
    void rejectsRelationshipChangesAfterCallEnds() {
        context.setDialogStatus(DialogProgressStatus.COMPLETED);
        SolutionCardRevision card = card(activeCallId, SolutionContextStatus.ACTIVE);
        card.setParentCardId(UUID.randomUUID());
        stubCard(card);

        assertThrows(IllegalStateException.class, () -> service.saveCardRevision(
                context.getId(), card.getCardId(),
                request(SolutionContextOperation.UNLINK, card, null)));

        verify(solutionContextRepository, never()).save(any());
    }

    @Test
    void rejectsCardOperationsForDdsContext() {
        context.setTargetType(IncidentTargetType.DDS);
        SolutionCardRevision card = card(activeCallId, SolutionContextStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> service.saveCardRevision(
                context.getId(), card.getCardId(),
                request(SolutionContextOperation.SAVE, card, null)));

        verify(solutionContextRepository, never()).save(any());
    }

    @Test
    void creatingDuplicateDoesNotReviseCanonicalCard() {
        allowSave();
        attachCall(activeCallId);
        SolutionCardRevision canonical = card(UUID.randomUUID(), SolutionContextStatus.ACTIVE);
        stubCard(canonical);
        SolutionContextRequest request = request(
                SolutionContextOperation.DUPLICATE, canonical, null);

        var response = service.createCardForCall(context.getId(), activeCallId, request);

        assertEquals(canonical.getCardId(), response.cardId());
        assertEquals(1, context.getSolutionCards().size());
        SolutionCardRevision duplicate = context.getSolutionCards().get(0);
        assertEquals(SolutionContextStatus.CLOSED_DUPLICATE, duplicate.getStatus());
        assertEquals(canonical.getCardId(), duplicate.getDuplicateOfCardId());
        verify(solutionContextRepository, times(1)).save(any());
    }

    private void allowSave() {
        when(solutionContextRepository.save(any())).thenAnswer(invocation -> {
            SolutionCardRevision revision = invocation.getArgument(0);
            if (revision.getId() == null) revision.setId(UUID.randomUUID());
            return revision;
        });
    }

    private SolutionContextRequest request(SolutionContextOperation operation,
                                           SolutionCardRevision card, UUID relationTargetId) {
        PersonInfoRequest applicant = new PersonInfoRequest(
                "79990000000", null, "Петров", "Пётр", null, "Москва", null);
        return new SolutionContextRequest(
                applicant, null, Map.of("floor", "5"), "FIRE",
                card.getCardId(), card.getVersion(), operation, relationTargetId);
    }

    private SolutionCardRevision card(UUID callId, SolutionContextStatus status) {
        SolutionCardRevision card = new SolutionCardRevision();
        card.setId(UUID.randomUUID());
        card.setContextId(context.getId());
        card.setCardId(UUID.randomUUID());
        card.setVersion(1);
        card.setCallId(callId);
        card.setStatus(status);
        return card;
    }

    private void stubCard(SolutionCardRevision card) {
        when(solutionContextRepository.findLatestByCard(
                context.getId(), card.getCardId())).thenReturn(Optional.of(card));
    }

    private void attachCall(UUID callId) {
        CallSnapshot call = new CallSnapshot();
        call.setSourceId(callId);
        StageSnapshot stage = new StageSnapshot();
        stage.getCalls().add(call);
        IncidentSnapshot incident = new IncidentSnapshot();
        incident.getStages().add(stage);
        context.getIncidents().add(incident);
    }
}
