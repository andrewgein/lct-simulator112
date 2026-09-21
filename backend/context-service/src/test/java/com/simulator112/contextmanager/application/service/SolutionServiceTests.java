package com.simulator112.contextmanager.application.service;

import com.simulator112.contextmanager.application.model.system112.PersonInfoRequest;
import com.simulator112.contextmanager.application.model.system112.SolutionContextRequest;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.application.port.out.SolutionCardStore;
import com.simulator112.contextmanager.domain.common.*;
import com.simulator112.contextmanager.domain.system112.PersonInfo;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import com.simulator112.contextmanager.domain.system112.SolutionContextOperation;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolutionServiceTests {
    @Mock private ContextStore contextStore;
    @Mock private SolutionCardStore cardStore;
    @InjectMocks private SolutionService service;
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
        when(contextStore.findById(context.getId())).thenReturn(Optional.of(context));
    }

    @Test
    void linksExistingCardWithoutChangingItsIdentity() {
        allowSave();
        SolutionCardRevision card = card(activeCallId);
        SolutionCardRevision target = card(UUID.randomUUID());
        stub(card); stub(target);

        service.saveCardRevision(context.getId(), card.getCardId(),
                request(SolutionContextOperation.LINK, card, target.getCardId()));

        SolutionCardRevision revision = context.getSolutionCards().getFirst();
        assertEquals(card.getCardId(), revision.getCardId());
        assertEquals(target.getCardId(), revision.getMainCardId());
        assertEquals(2, revision.getVersion());
    }

    @Test
    void createsNewSubordinateCardLinkedToMainCard() {
        allowSave();
        attachCall(activeCallId);
        SolutionCardRevision target = card(UUID.randomUUID());
        stub(target);
        var request = new SolutionContextRequest(person(), null, Map.of(), "FIRE",
                null, null, SolutionContextOperation.LINK, target.getCardId());

        var response = service.createCardForCall(context.getId(), activeCallId, request);

        assertNotEquals(target.getCardId(), response.cardId());
        assertEquals(target.getCardId(), context.getSolutionCards().getFirst().getMainCardId());
        verify(cardStore).save(any());
    }

    @Test
    void unlinkPreservesCardData() {
        allowSave();
        SolutionCardRevision card = card(activeCallId);
        card.setMainCardId(UUID.randomUUID());
        card.setApplicant(new PersonInfo("79990000000", null, "Иванов", "Иван", null, "Москва", null));
        card.setIncidentType("FIRE");
        card.setAdditionalInfoProvided(true);
        card.getAdditionalInfo().put("floor", "3");
        stub(card);

        service.saveCardRevision(context.getId(), card.getCardId(),
                request(SolutionContextOperation.UNLINK, card, null));

        SolutionCardRevision revision = context.getSolutionCards().getFirst();
        assertNull(revision.getMainCardId());
        when(cardStore.findAll(context.getId())).thenReturn(List.of(card, revision));
        var assembled = service.getCards(context.getId()).getFirst();
        assertEquals("Иван", assembled.applicant().firstName());
        assertEquals(Map.of("floor", "3"), assembled.additionalInfo());
    }

    @Test
    void rejectsRelationshipChangesAfterCallEnds() {
        context.setDialogStatus(DialogProgressStatus.COMPLETED);
        SolutionCardRevision card = card(activeCallId);
        card.setMainCardId(UUID.randomUUID());
        stub(card);

        assertThrows(IllegalStateException.class, () -> service.saveCardRevision(
                context.getId(), card.getCardId(), request(SolutionContextOperation.UNLINK, card, null)));
        verify(cardStore, never()).save(any());
    }

    @Test
    void rejectsCardOperationsForDdsContext() {
        context.setTargetType(IncidentTargetType.DDS);
        SolutionCardRevision card = card(activeCallId);
        assertThrows(IllegalArgumentException.class, () -> service.saveCardRevision(
                context.getId(), card.getCardId(), request(SolutionContextOperation.SAVE, card, null)));
    }

    private void allowSave() {
        when(cardStore.save(any())).thenAnswer(invocation -> {
            SolutionCardRevision revision = invocation.getArgument(0);
            if (revision.getId() == null) revision.setId(UUID.randomUUID());
            return revision;
        });
    }

    private SolutionContextRequest request(SolutionContextOperation operation,
                                           SolutionCardRevision card, UUID mainCardId) {
        return new SolutionContextRequest(person(), null, Map.of("floor", "5"), "FIRE",
                card.getCardId(), card.getVersion(), operation, mainCardId);
    }

    private PersonInfoRequest person() {
        return new PersonInfoRequest("79990000000", null, "Петров", "Пётр", null, "Москва", null);
    }

    private SolutionCardRevision card(UUID callId) {
        SolutionCardRevision card = new SolutionCardRevision();
        card.setId(UUID.randomUUID()); card.setContextId(context.getId()); card.setCardId(UUID.randomUUID());
        card.setVersion(1); card.setCallId(callId);
        return card;
    }

    private void stub(SolutionCardRevision card) {
        when(cardStore.findLatestByCard(context.getId(), card.getCardId())).thenReturn(Optional.of(card));
    }

    private void attachCall(UUID callId) {
        CallSnapshot call = new CallSnapshot(); call.setSourceId(callId);
        StageSnapshot stage = new StageSnapshot(); stage.getCalls().add(call);
        IncidentSnapshot incident = new IncidentSnapshot(); incident.getStages().add(stage);
        context.getIncidents().add(incident);
    }
}
