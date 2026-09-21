package com.simulator112.incident.application.service;

import com.simulator112.incident.application.port.in.CreateLevelUseCase;
import com.simulator112.incident.application.port.in.GetLevelUseCase;
import com.simulator112.incident.application.port.in.UpdateLevelUseCase;
import com.simulator112.incident.application.port.out.IncidentRepository;
import com.simulator112.incident.application.port.out.LevelRepository;
import com.simulator112.incident.domain.level.Level;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LevelApplicationService implements CreateLevelUseCase, GetLevelUseCase, UpdateLevelUseCase {
    private final LevelRepository levelRepository;
    private final IncidentRepository incidentRepository;

    @Override
    @Transactional
    public Level createLevel(Level level) {
        validate(level);
        return levelRepository.save(level);
    }

    @Override
    @Transactional
    public Level updateLevel(UUID levelId, Level level) {
        if (levelRepository.findById(levelId).isEmpty()) {
            throw new IllegalArgumentException("Уровень не найден: " + levelId);
        }
        Level updated = new Level(levelId, level.title(), level.targetType(), level.difficulty(),
                level.executionMode(), level.incidentIds());
        validate(updated);
        return levelRepository.save(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Level getLevel(UUID levelId) {
        return levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Уровень не найден: " + levelId));
    }

    private void validate(Level level) {
        if (level == null || level.title() == null || level.title().isBlank()) {
            throw new IllegalArgumentException("Название уровня обязательно");
        }
        if (level.targetType() == null || level.difficulty() == null || level.executionMode() == null) {
            throw new IllegalArgumentException("Тип, сложность и режим выполнения уровня обязательны");
        }
        if (level.incidentIds().isEmpty()) {
            throw new IllegalArgumentException("Уровень должен содержать хотя бы один инцидент");
        }
        if (new HashSet<>(level.incidentIds()).size() != level.incidentIds().size()) {
            throw new IllegalArgumentException("Инцидент не может повторяться внутри уровня");
        }
        for (UUID incidentId : level.incidentIds()) {
            var incident = incidentRepository.findById(incidentId)
                    .orElseThrow(() -> new IllegalArgumentException("Инцидент не найден: " + incidentId));
            if (incident.targetType() != level.targetType()) {
                throw new IllegalArgumentException("Уровень не может смешивать SYSTEM_112 и DDS инциденты");
            }
            if (incident.difficulty() != level.difficulty()) {
                throw new IllegalArgumentException("Сложность инцидента не совпадает со сложностью уровня");
            }
        }
    }
}
