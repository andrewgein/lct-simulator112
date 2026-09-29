package com.simulator112.contextmanager.application.service;

import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.domain.common.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DialogServiceDdsCallsTests {
    private final ContextStore store = mock(ContextStore.class);
    private final DialogService service = new DialogService(store);

    @Test
    void ddsCallUsesAddressOfItsOwnIncident() {
        var fixture = fixture(IncidentTargetType.DDS);
        var address = new Address("Москва", "Тверская", "8", "1", "5", 4);
        fixture.context.getIncidents().getFirst().setAddress(address);
        var otherIncident = new IncidentSnapshot();
        otherIncident.setSourceId(UUID.randomUUID());
        otherIncident.setAddress(new Address("Москва", "Петровка", "1", null, null, null));
        fixture.context.getIncidents().addFirst(otherIncident);
        fixture.context.setActiveCallId(fixture.previous.getSourceId());
        fixture.context.setDialogStatus(DialogProgressStatus.IN_CALL);

        assertThat(service.getCall(fixture.context.getId().toString(), fixture.previous.getSourceId().toString()).incidentAddress())
                .isEqualTo(address);
        assertThat(service.getNextCall(fixture.context.getId().toString(), "-1").incidentAddress()).isEqualTo(address);
    }

    @Test
    void system112CallDoesNotExposeIncidentAddress() {
        var fixture = fixture(IncidentTargetType.SYSTEM_112);
        fixture.context.getIncidents().getFirst().setAddress(new Address("Москва", "Тверская", "8", null, null, null));

        assertThat(service.getNextCall(fixture.context.getId().toString(), "-1").incidentAddress()).isNull();
    }

    @Test
    void canStartNextDdsCallAfterPreviousWasMissed() {
        var fixture = fixture(IncidentTargetType.DDS);
        fixture.context.setActiveCallId(fixture.previous.getSourceId());
        fixture.context.setDialogStatus(DialogProgressStatus.DISCONNECTED);

        var progress = service.startCall(fixture.context.getId().toString(), fixture.next.getSourceId().toString());

        assertThat(progress.activeCallId()).isEqualTo(fixture.next.getSourceId());
        assertThat(fixture.next.getStatus()).isEqualTo(CallStatus.ACTIVE);
    }

    @Test
    void ongoingDdsCallRemainsAvailableAfterTimelineMovesOn() {
        var fixture = fixture(IncidentTargetType.DDS);
        fixture.context.setActiveCallId(fixture.previous.getSourceId());
        fixture.context.setDialogStatus(DialogProgressStatus.IN_CALL);
        fixture.context.getIncidents().getFirst().setActiveStageId(UUID.randomUUID());
        fixture.context.getIncidents().getFirst().setStatus(IncidentProgressStatus.COMPLETED);

        assertThat(service.getCall(fixture.context.getId().toString(), fixture.previous.getSourceId().toString()).call())
                .isSameAs(fixture.previous);
        service.completeCall(fixture.context.getId().toString(), fixture.previous.getSourceId().toString());
        assertThat(fixture.previous.getStatus()).isEqualTo(CallStatus.COMPLETED);
    }

    @Test
    void system112StillCannotStartDifferentCallWhileDisconnected() {
        var fixture = fixture(IncidentTargetType.SYSTEM_112);
        fixture.context.setActiveCallId(fixture.previous.getSourceId());
        fixture.context.setDialogStatus(DialogProgressStatus.DISCONNECTED);

        assertThatThrownBy(() -> service.startCall(fixture.context.getId().toString(), fixture.next.getSourceId().toString()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Другой звонок");
    }

    private Fixture fixture(IncidentTargetType targetType) {
        var context = new TrainingContext();
        context.setId(UUID.randomUUID());
        context.setTargetType(targetType);
        var incident = new IncidentSnapshot();
        incident.setSourceId(UUID.randomUUID());
        incident.setStatus(IncidentProgressStatus.ACTIVE);
        var stage = new StageSnapshot();
        stage.setSourceId(UUID.randomUUID());
        incident.setActiveStageId(stage.getSourceId());
        var previous = new CallSnapshot();
        previous.setSourceId(UUID.randomUUID());
        previous.setQueuePosition(0);
        previous.setStatus(CallStatus.DISCONNECTED);
        var next = new CallSnapshot();
        next.setSourceId(UUID.randomUUID());
        next.setQueuePosition(1);
        next.setStatus(CallStatus.PENDING);
        stage.getCalls().add(previous);
        stage.getCalls().add(next);
        incident.getStages().add(stage);
        context.getIncidents().add(incident);
        when(store.findById(context.getId())).thenReturn(Optional.of(context));
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new Fixture(context, previous, next);
    }

    private record Fixture(TrainingContext context, CallSnapshot previous, CallSnapshot next) {
    }
}
