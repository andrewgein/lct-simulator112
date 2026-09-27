package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.CreateIncidentUseCase;
import com.simulator112.incident.application.port.in.FindAvailableIncidentsUseCase;
import com.simulator112.incident.application.port.in.GetIncidentUseCase;
import com.simulator112.incident.application.port.in.UpdateIncidentUseCase;
import com.simulator112.incident.application.port.out.ClassifierCatalogPort;
import com.simulator112.incident.application.port.out.IncidentRepository;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.common.exception.IncidentNotFoundException;
import com.simulator112.incident.domain.dds.DdsIncident;
import com.simulator112.incident.domain.system112.System112Incident;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentApplicationService implements CreateIncidentUseCase, UpdateIncidentUseCase,
        GetIncidentUseCase, FindAvailableIncidentsUseCase {

    private final IncidentRepository incidentRepository;
    private final ClassifierCatalogPort classifierCatalog;

    @Override
    public Incident createIncident(Incident incident) {
        validate(incident);
        return incidentRepository.save(incident);
    }

    @Override
    public Incident updateIncident(UUID incidentId, Incident incident) {
        getIncident(incidentId);
        if (incident.id() != null && !incidentId.equals(incident.id())) {
            throw new IllegalArgumentException("Идентификатор происшествия нельзя изменить");
        }
        validate(incident);
        return incidentRepository.save(incident);
    }

    @Override
    public Incident getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    @Override
    public List<Incident> findAvailableIncidents(IncidentTargetType targetType, Difficulty difficulty) {
        return incidentRepository.findAvailable(targetType, difficulty);
    }

    private void validate(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("Происшествие обязательно");
        }
        if (incident.stages().isEmpty()) {
            throw new IllegalArgumentException("Происшествие должно содержать хотя бы один этап");
        }
        if (incident instanceof System112Incident system112) {
            system112.stages().forEach(stage -> {
                requireClassifierCodes(stage.classifierCodes());
                if (stage.expectedRoutingFacts().entrySet().stream().anyMatch(entry -> entry.getKey().isBlank() || entry.getValue().isBlank())) {
                    throw new IllegalArgumentException("Ожидаемые ответы должны содержать код признака и значение");
                }
            });
            if (system112.stages().stream()
                    .flatMap(stage -> stage.calls().stream())
                    .noneMatch(call -> call.counterparty()
                            == com.simulator112.incident.domain.common.CounterpartyType.CALLER)) {
                throw new IllegalArgumentException("Сценарий оператора должен содержать звонок заявителю");
            }
        }
        if (incident instanceof DdsIncident dds) {
            if (dds.preparedCardTemplate() == null || dds.initialAssignment() == null) {
                throw new IllegalArgumentException("Сценарий ДДС требует карточку и первичное назначение");
            }
            if (dds.initialAssignment().emergencyService() == null || dds.initialAssignment().emergencyService().isBlank()) {
                throw new IllegalArgumentException("Служба первичного назначения обязательна");
            }
            classifierCatalog.requireService(dds.initialAssignment().emergencyService());
            requireClassifierCodes(dds.preparedCardTemplate().classifierCodes());
            requireClassifierCode(dds.initialAssignment().classifierCode());
            validateDdsStageCalls(dds);
            validateDdsTree(dds);
        }
    }

    private void requireClassifierCodes(List<String> classifierCodes) {
        if (classifierCodes.isEmpty()) {
            throw new IllegalArgumentException("Необходимо указать хотя бы один тип происшествия");
        }
        if (classifierCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
            throw new IllegalArgumentException("Тип происшествия не может быть пустым");
        }
        if (classifierCodes.stream().distinct().count() != classifierCodes.size()) {
            throw new IllegalArgumentException("Типы происшествия не должны повторяться");
        }
        classifierCodes.forEach(this::requireClassifierCode);
    }

    private void requireClassifierCode(String classifierCode) {
        if (classifierCode == null || classifierCode.isBlank()) {
            throw new IllegalArgumentException("Тип происшествия не может быть пустым");
        }
        classifierCatalog.requireEntry(classifierCode);
    }

    private void validateDdsStageCalls(DdsIncident incident) {
        for (var stage : incident.stages()) {
            if (stage.type() == com.simulator112.incident.domain.dds.DdsStageType.CALL_BRIGADE_FOR_STATUS) {
                if (stage.calls().isEmpty() || stage.calls().stream().anyMatch(call ->
                        call.direction() != com.simulator112.incident.domain.common.CallDirection.OUTBOUND
                                || call.counterparty()
                                != com.simulator112.incident.domain.common.CounterpartyType.BRIGADE)) {
                    throw new IllegalArgumentException(
                            "Этап уточнения статуса должен содержать только исходящие звонки бригаде");
                }
            } else if (!stage.calls().isEmpty()) {
                throw new IllegalArgumentException(
                        "Звонки разрешены только на этапе уточнения статуса бригады");
            }
        }
    }

    private void validateDdsTree(DdsIncident incident) {
        var stageIds = incident.stages().stream().map(stage -> stage.id()).collect(java.util.stream.Collectors.toSet());
        if (stageIds.contains(null)) {
            throw new IllegalArgumentException("Этапы ДДС должны иметь идентификаторы");
        }
        if (stageIds.size() != incident.stages().size()) {
            throw new IllegalArgumentException("Идентификаторы этапов ДДС должны быть уникальны");
        }
        if (incident.initialStageId() == null || !stageIds.contains(incident.initialStageId())) {
            throw new IllegalArgumentException("Начальный этап ДДС не найден");
        }
        var initialStage = incident.stages().stream()
                .filter(stage -> stage.id().equals(incident.initialStageId()))
                .findFirst()
                .orElseThrow();
        if (initialStage.type() != com.simulator112.incident.domain.dds.DdsStageType.ASSIGN_BRIGADE) {
            throw new IllegalArgumentException("Первый этап ДДС должен подтверждать принятие карточки");
        }
        if (initialStage.timeLimitSeconds() != 30) {
            throw new IllegalArgumentException("На принятие карточки ДДС должно отводиться 30 секунд");
        }
        long acceptanceStages = incident.stages().stream()
                .filter(stage -> stage.type()
                        == com.simulator112.incident.domain.dds.DdsStageType.ASSIGN_BRIGADE)
                .count();
        if (acceptanceStages != 1) {
            throw new IllegalArgumentException("Этап принятия карточки ДДС должен быть только первым");
        }

        var routes = new java.util.HashMap<UUID, com.simulator112.incident.domain.dds.DdsStageTransition>();
        var incoming = new java.util.HashMap<UUID, Integer>();
        for (var transition : incident.transitions()) {
            if (!stageIds.contains(transition.stageId()) || routes.put(transition.stageId(), transition) != null) {
                throw new IllegalArgumentException("Некорректный или повторяющийся переход этапа ДДС");
            }
            for (UUID next : java.util.stream.Stream.of(
                            transition.successStageId(), transition.failureStageId())
                    .filter(java.util.Objects::nonNull).distinct().toList()) {
                if (!stageIds.contains(next)) {
                    throw new IllegalArgumentException("Переход ссылается на неизвестный этап " + next);
                }
                if (incoming.merge(next, 1, Integer::sum) > 1) {
                    throw new IllegalArgumentException("Этап ДДС не может иметь более одного родителя");
                }
            }
        }
        if (incoming.containsKey(incident.initialStageId())) {
            throw new IllegalArgumentException("Начальный этап ДДС не может иметь родителя");
        }

        var visited = new java.util.HashSet<UUID>();
        walkDdsTree(incident.initialStageId(), routes, new java.util.HashSet<>(), visited);
        if (!visited.equals(stageIds)) {
            throw new IllegalArgumentException("Все этапы ДДС должны быть достижимы из начального этапа");
        }
    }

    private void walkDdsTree(
            UUID stageId,
            java.util.Map<UUID, com.simulator112.incident.domain.dds.DdsStageTransition> routes,
            java.util.Set<UUID> path,
            java.util.Set<UUID> visited) {
        if (!path.add(stageId)) {
            throw new IllegalArgumentException("Дерево этапов ДДС не должно содержать циклы");
        }
        visited.add(stageId);
        var transition = routes.get(stageId);
        if (transition != null) {
            if (transition.successStageId() != null) {
                walkDdsTree(transition.successStageId(), routes, path, visited);
            }
            if (transition.failureStageId() != null) {
                walkDdsTree(transition.failureStageId(), routes, path, visited);
            }
        }
        path.remove(stageId);
    }
}
