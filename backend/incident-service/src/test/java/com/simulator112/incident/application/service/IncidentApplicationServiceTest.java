package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentRepository;
import com.simulator112.incident.domain.common.*;
import com.simulator112.incident.domain.common.exception.IncidentNotFoundException;
import com.simulator112.incident.domain.dds.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentApplicationServiceTest {
    private final IncidentRepository repository = mock(IncidentRepository.class);
    private final ClassifierCatalogPort classifier = mock(ClassifierCatalogPort.class);
    private final IncidentApplicationService service = new IncidentApplicationService(repository, classifier);

    @Test
    void deletesExistingIncident() {
        UUID incidentId = UUID.randomUUID();
        var incident = incident(List.of(acceptanceStage(UUID.randomUUID())));
        when(repository.findById(incidentId)).thenReturn(Optional.of(incident));

        service.deleteIncident(incidentId);

        verify(repository).deleteById(incidentId);
    }

    @Test
    void rejectsDeletingMissingIncident() {
        UUID incidentId = UUID.randomUUID();
        when(repository.findById(incidentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteIncident(incidentId)).isInstanceOf(IncidentNotFoundException.class);

        verify(repository, never()).deleteById(incidentId);
    }

    @Test
    void acceptsOrderedStages() {
        UUID root = UUID.randomUUID();
        UUID success = UUID.randomUUID();
        UUID failure = UUID.randomUUID();
        DdsIncident incident = incident(
                List.of(acceptanceStage(root), stage(success), stage(failure)));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);
    }

    @Test
    void acceptsTwoOrderedStages() {
        UUID root = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        DdsIncident incident = incident(
                List.of(acceptanceStage(root), stage(next)));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);
    }

    @Test
    void rejectsDdsScenarioWithoutAcceptanceAsInitialStage() {
        UUID root = UUID.randomUUID();
        var stage = new DdsStage(root, "Завершение", null,
                DdsStageType.COMPLETE_INCIDENT, 30, List.of());
        DdsIncident incident = incident(List.of(stage));

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Первый этап ДДС должен подтверждать принятие карточки");
    }

    @Test
    void rejectsAcceptanceStageWithWrongTimeLimit() {
        UUID root = UUID.randomUUID();
        var stage = new DdsStage(root, "Подтверждение получения", null,
                DdsStageType.ASSIGN_BRIGADE, 60, List.of());
        DdsIncident incident = incident(List.of(stage));

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("30 секунд");
    }

    @Test
    void rejectsAcceptanceStageOutsideFirstPosition() {
        UUID root = UUID.randomUUID();
        UUID duplicate = UUID.randomUUID();
        DdsIncident incident = incident(
                List.of(acceptanceStage(root), acceptanceStage(duplicate)));

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("должен быть только первым");
    }

    @Test
    void acceptsCallOnAnyDdsStage() {
        UUID root = UUID.randomUUID();
        var call = new CallScenario(null, 0, CallDirection.OUTBOUND, CounterpartyType.BRIGADE,
                null, null, List.of(), List.of(), null, null);
        var stage = new DdsStage(root, "Назначение бригады", null,
                DdsStageType.ASSIGN_BRIGADE, 30, List.of(call));
        DdsIncident incident = incident(List.of(stage));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);
    }

    @Test
    void permitsIncomingBrigadeAndIncomingOtherServiceCalls() {
        UUID root = UUID.randomUUID();
        var incoming = new CallScenario(null, 0, CallDirection.INBOUND, CounterpartyType.BRIGADE,
                null, null, List.of(), List.of(), null, null);
        var otherService = new CallScenario(null, 1, CallDirection.INBOUND, CounterpartyType.SERVICE,
                null, null, List.of(), List.of(), null, null, "POLICE");
        var contactStage = new DdsStage(UUID.randomUUID(), "Связь со службами", null,
                DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, 60, List.of(incoming, otherService));
        var incident = incident(List.of(acceptanceStage(root), contactStage));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);
    }

    @Test
    void rejectsOtherServiceCallWithoutServiceCode() {
        var call = new CallScenario(null, 0, CallDirection.INBOUND, CounterpartyType.SERVICE,
                null, null, List.of(), List.of(), null, null);
        var incident = incident(List.of(acceptanceStage(UUID.randomUUID()),
                new DdsStage(UUID.randomUUID(), "Звонок", null,
                        DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, 60, List.of(call))));

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("выберите службу");
    }

    @Test
    void permitsStatusStageWithoutCall() {
        UUID root = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        var stage = new DdsStage(next, "Уточнение статуса", null,
                DdsStageType.CALL_BRIGADE_FOR_STATUS, 60, List.of(), null);
        DdsIncident incident = incident(List.of(acceptanceStage(root), stage));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);
    }

    @Test
    void acceptsOrderedStagesWithoutBranches() {
        UUID root = UUID.randomUUID();
        UUID unreachable = UUID.randomUUID();
        DdsIncident incident = incident(List.of(acceptanceStage(root), stage(unreachable)));

        when(repository.save(incident)).thenReturn(incident);
        service.createIncident(incident);
    }

    @Test
    void requiresPreparedCardClassifierCodes() {
        UUID root = UUID.randomUUID();
        DdsIncident incident = incident(List.of(stage(root)), List.of());

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("хотя бы один тип происшествия");
    }

    @Test
    void rejectsDuplicatePreparedCardClassifierCodes() {
        UUID root = UUID.randomUUID();
        DdsIncident incident = incident(List.of(stage(root)), List.of("101", "101"));

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не должны повторяться");
    }

    @Test
    void validatesPreparedCardClassifierCodes() {
        UUID root = UUID.randomUUID();
        DdsIncident incident = incident(List.of(acceptanceStage(root)));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);

        verify(classifier).requireEntry("101");
        verify(classifier).requireService("MCHS");
    }

    @Test
    void acceptsInitialAssignmentWithoutClassifierCode() {
        DdsIncident incident = incident(List.of(acceptanceStage(UUID.randomUUID())));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);

        verify(classifier).requireEntry("101");
    }

    private DdsIncident incident(List<DdsStage> stages) {
        return incident(stages, List.of("101"));
    }

    private DdsIncident incident(List<DdsStage> stages, List<String> classifierCodes) {
        return new DdsIncident(null, "Пожар", new Address("Москва", "Тверская", "1", null, null, 1),
                Difficulty.NORMAL, stages, new PreparedCardTemplate(classifierCodes, null, 0, Map.of(), List.of()),
                new InitialAssignment("MCHS"));
    }

    private DdsStage acceptanceStage(UUID id) {
        return new DdsStage(id, "Подтверждение получения", null,
                DdsStageType.ASSIGN_BRIGADE, 30, List.of());
    }

    private DdsStage stage(UUID id) {
        return new DdsStage(id, "Ожидание статуса", null,
                DdsStageType.WAIT_FOR_BRIGADE_STATUS_CHANGE, 60, List.of());
    }
}
