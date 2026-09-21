package com.simulator112.incident.adapter.out.persistence;

import com.simulator112.incident.adapter.out.persistence.entity.common.LevelJpaEntity;
import com.simulator112.incident.adapter.out.persistence.repository.SpringDataIncidentRepository;
import com.simulator112.incident.adapter.out.persistence.repository.SpringDataLevelRepository;
import com.simulator112.incident.application.port.out.LevelRepository;
import com.simulator112.incident.domain.level.Level;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class LevelPersistenceAdapter implements LevelRepository {
    private final SpringDataLevelRepository levelRepository;
    private final SpringDataIncidentRepository incidentRepository;

    @Override
    @Transactional
    public Level save(Level level) {
        LevelJpaEntity entity = new LevelJpaEntity();
        entity.setId(level.id());
        entity.setTitle(level.title());
        entity.setTargetType(level.targetType());
        entity.setDifficulty(level.difficulty());
        entity.setExecutionMode(level.executionMode());
        entity.setIncidents(level.incidentIds().stream()
                .map(incidentRepository::getReferenceById)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return toDomain(levelRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Level> findById(UUID levelId) {
        return levelRepository.findById(levelId).map(this::toDomain);
    }

    private Level toDomain(LevelJpaEntity entity) {
        return new Level(entity.getId(), entity.getTitle(), entity.getTargetType(), entity.getDifficulty(),
                entity.getExecutionMode(), entity.getIncidents().stream().map(incident -> incident.getId()).toList());
    }
}
