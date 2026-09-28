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
            system112.stages().forEach(stage -> requireClassifierCodes(stage.classifierCodes()));
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
            var assignedServices = dds.preparedCardTemplate().assignedServices();
            if (assignedServices.stream().anyMatch(code -> code == null || code.isBlank()) || assignedServices.stream().distinct().count() != assignedServices.size()) {
                throw new IllegalArgumentException("Назначенные службы не должны быть пустыми или повторяться");
            }
            assignedServices.forEach(classifierCatalog::requireService);
            validateDdsStageCalls(dds);
            validateDdsTimeline(dds);
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
            if (stage.expectedComment() != null && !stage.expectedComment().isBlank() && stage.calls().isEmpty()) {
                throw new IllegalArgumentException("Для ожидаемого комментария необходим звонок");
            }
            for (var call : stage.calls()) {
                if (call.direction() == null || (call.counterparty() != com.simulator112.incident.domain.common.CounterpartyType.BRIGADE
                        && call.counterparty() != com.simulator112.incident.domain.common.CounterpartyType.SERVICE)) {
                    throw new IllegalArgumentException("На этапе ДДС можно звонить бригаде или другой службе");
                }
                if (call.counterparty() == com.simulator112.incident.domain.common.CounterpartyType.SERVICE) {
                    if (call.serviceCode() == null || call.serviceCode().isBlank()) {
                        throw new IllegalArgumentException("Для звонка другой службе выберите службу");
                    }
                    if (call.serviceCode().equals(incident.initialAssignment().emergencyService())) {
                        throw new IllegalArgumentException("Для звонка выберите другую службу, а не службу ДДС");
                    }
                    classifierCatalog.requireService(call.serviceCode());
                }
            }
        }
    }

    private void validateDdsTimeline(DdsIncident incident) {
        var stageIds = incident.stages().stream().map(stage -> stage.id()).collect(java.util.stream.Collectors.toSet());
        if (stageIds.contains(null)) {
            throw new IllegalArgumentException("Этапы ДДС должны иметь идентификаторы");
        }
        if (stageIds.size() != incident.stages().size()) {
            throw new IllegalArgumentException("Идентификаторы этапов ДДС должны быть уникальны");
        }
        if (incident.stages().isEmpty()) {
            throw new IllegalArgumentException("ДДС должен содержать этапы");
        }
        var initialStage = incident.stages().getFirst();
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

        if (incident.stages().stream().anyMatch(stage -> stage.timeLimitSeconds() <= 0)) {
            throw new IllegalArgumentException("Длительность этапа ДДС должна быть положительной");
        }
    }

}
