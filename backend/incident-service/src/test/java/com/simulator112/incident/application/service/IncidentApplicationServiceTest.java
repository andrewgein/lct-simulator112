package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentRepository;
import com.simulator112.incident.domain.common.*;
import com.simulator112.incident.domain.dds.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentApplicationServiceTest {
    private final IncidentRepository repository = mock(IncidentRepository.class);
    private final ClassifierCatalogPort classifier = mock(ClassifierCatalogPort.class);
    private final IncidentApplicationService service = new IncidentApplicationService(repository, classifier);

    @Test
    void acceptsDdsBinaryTree() {
        UUID root = UUID.randomUUID();
        UUID success = UUID.randomUUID();
        UUID failure = UUID.randomUUID();
        DdsIncident incident = incident(
                List.of(stage(root), stage(success), stage(failure)),
                root,
                List.of(new DdsStageTransition(root, success, failure)));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);
    }

    @Test
    void acceptsTransitionWithSameSuccessAndFailureStage() {
        UUID root = UUID.randomUUID();
        UUID next = UUID.randomUUID();
        DdsIncident incident = incident(
                List.of(stage(root), stage(next)),
                root,
                List.of(new DdsStageTransition(root, next, next)));
        when(repository.save(incident)).thenReturn(incident);

        service.createIncident(incident);
    }

    @Test
    void rejectsCallsOutsideStatusClarificationStage() {
        UUID root = UUID.randomUUID();
        var call = new CallScenario(null, 0, CallDirection.OUTBOUND, CounterpartyType.BRIGADE,
                null, null, List.of(), List.of(), null, null);
        var stage = new DdsStage(root, "Назначение бригады", null,
                DdsStageType.ASSIGN_BRIGADE, 60, List.of(call));
        DdsIncident incident = incident(List.of(stage), root, List.of());

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Звонки разрешены только");
    }

    @Test
    void requiresOutgoingBrigadeCallOnStatusClarificationStage() {
        UUID root = UUID.randomUUID();
        var stage = new DdsStage(root, "Уточнение статуса", null,
                DdsStageType.CALL_BRIGADE_FOR_STATUS, 60, List.of());
        DdsIncident incident = incident(List.of(stage), root, List.of());

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("исходящие звонки бригаде");
    }

    @Test
    void rejectsUnreachableDdsStage() {
        UUID root = UUID.randomUUID();
        UUID unreachable = UUID.randomUUID();
        DdsIncident incident = incident(List.of(stage(root), stage(unreachable)), root, List.of());

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("достижимы");
    }

    @Test
    void requiresPreparedCardClassifierCodes() {
        UUID root = UUID.randomUUID();
        DdsIncident incident = incident(List.of(stage(root)), root, List.of(), List.of());

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("хотя бы один тип происшествия");
    }

    @Test
    void rejectsDuplicatePreparedCardClassifierCodes() {
        UUID root = UUID.randomUUID();
        DdsIncident incident = incident(List.of(stage(root)), root, List.of(), List.of("101", "101"));

        assertThatThrownBy(() -> service.createIncident(incident))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не должны повторяться");
    }

    private DdsIncident incident(List<DdsStage> stages, UUID initialStageId,
                                 List<DdsStageTransition> transitions) {
        return incident(stages, initialStageId, transitions, List.of("101"));
    }

    private DdsIncident incident(List<DdsStage> stages, UUID initialStageId,
                                 List<DdsStageTransition> transitions, List<String> classifierCodes) {
        return new DdsIncident(null, "Пожар", new Address("Москва", "Тверская", "1", null, null, 1),
                Difficulty.NORMAL, stages, new PreparedCardTemplate(classifierCodes, null, null, Map.of()),
                new InitialAssignment(com.simulator112.incident.domain.common.EmergencyService.FIRE, "101", null),
                new DdsCriteria(List.of("Адрес?"), List.of("Назначить бригаду"), List.of()),
                initialStageId, transitions);
    }

    private DdsStage stage(UUID id) {
        var call = new CallScenario(null, 0, CallDirection.OUTBOUND, CounterpartyType.BRIGADE,
                null, null, List.of(), List.of(), null, null);
        return new DdsStage(id, "Уточнение статуса", null,
                DdsStageType.CALL_BRIGADE_FOR_STATUS, 60, List.of(call));
    }
}
