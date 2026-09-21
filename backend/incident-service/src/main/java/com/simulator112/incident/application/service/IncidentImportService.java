package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.CreateIncidentUseCase;
import com.simulator112.incident.application.port.in.CreateLevelUseCase;
import com.simulator112.incident.application.port.in.ImportIncidentsUseCase;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.common.exception.ClassifierEntryNotFoundException;
import com.simulator112.incident.domain.level.ExecutionMode;
import com.simulator112.incident.domain.level.Level;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentImportService implements ImportIncidentsUseCase {
    private final CreateIncidentUseCase createIncident;
    private final CreateLevelUseCase createLevel;

    @Override
    @Transactional
    public ImportedIncidents importIncidents(List<Incident> incidents, boolean createLevels) {
        if (incidents == null || incidents.isEmpty()) {
            throw new IllegalArgumentException("Список инцидентов для загрузки пуст");
        }
        List<Incident> saved = new ArrayList<>();
        for (int index = 0; index < incidents.size(); index++) {
            Incident incident = incidents.get(index);
            try {
                saved.add(createIncident.createIncident(incident));
            } catch (IllegalArgumentException | ClassifierEntryNotFoundException exception) {
                throw new IllegalArgumentException("Инцидент #" + (index + 1) + " «" + incident.title() + "»: "
                        + exception.getMessage(), exception);
            }
        }
        return new ImportedIncidents(saved, createLevels ? createLevels(saved) : List.of());
    }

    private List<Level> createLevels(List<Incident> incidents) {
        Map<LevelKey, List<UUID>> groups = new LinkedHashMap<>();
        incidents.forEach(incident -> groups
                .computeIfAbsent(new LevelKey(incident.targetType(), incident.difficulty()), ignored -> new ArrayList<>())
                .add(incident.id()));
        return groups.entrySet().stream()
                .map(group -> createLevel.createLevel(new Level(null, title(group.getKey()), group.getKey().targetType(),
                        group.getKey().difficulty(), ExecutionMode.SEQUENTIAL, group.getValue())))
                .toList();
    }

    private String title(LevelKey key) {
        String target = switch (key.targetType()) {
            case SYSTEM_112 -> "Система 112";
            case DDS -> "ДДС";
        };
        String difficulty = switch (key.difficulty()) {
            case EASY -> "лёгкий";
            case NORMAL -> "средний";
            case HARD -> "сложный";
        };
        return target + " — " + difficulty;
    }

    private record LevelKey(IncidentTargetType targetType, Difficulty difficulty) {
    }
}
