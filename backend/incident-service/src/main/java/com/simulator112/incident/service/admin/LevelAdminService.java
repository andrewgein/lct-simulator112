package com.simulator112.incident.service.admin;

import com.simulator112.incident.dto.request.CreateLevelRequest;
import com.simulator112.incident.dto.request.UpdateLevelRequest;
import com.simulator112.incident.dto.view.LevelView;
import com.simulator112.incident.exception.IncidentNotFoundException;
import com.simulator112.incident.exception.ResourceNotFoundException;
import com.simulator112.incident.mapper.IncidentMapper;
import com.simulator112.incident.model.entity.IncidentEntity;
import com.simulator112.incident.model.entity.LevelEntity;
import com.simulator112.incident.repository.IncidentRepository;
import com.simulator112.incident.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LevelAdminService {

    private final LevelRepository levelRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;

    @Transactional(readOnly = true)
    public List<LevelView> getLevels() {

        return levelRepository.findAll().stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public LevelView getLevel(UUID id) {

        return toView(getLevelEntity(id));
    }

    @Transactional
    public LevelView createLevel(CreateLevelRequest request) {

        LevelEntity level = LevelEntity.builder()
                .title(request.title())
                .difficulty(request.difficulty())
                .build();

        return toView(levelRepository.save(level));
    }

    @Transactional
    public LevelView updateLevel(UUID id, UpdateLevelRequest request) {

        LevelEntity level = getLevelEntity(id);

        if (request.title() != null) {
            level.setTitle(request.title());
        }
        if (request.difficulty() != null) {
            level.setDifficulty(request.difficulty());
        }

        return toView(level);
    }

    @Transactional
    public void deleteLevel(UUID id) {
        levelRepository.delete(getLevelEntity(id));
    }

    @Transactional
    public LevelView attachIncident(UUID levelId, UUID incidentId) {

        LevelEntity level = getLevelEntity(levelId);

        IncidentEntity incident = getIncident(incidentId);

        if (incident.getLevel() != null) {
            incident.getLevel().removeIncident(incident);
        }

        level.addIncident(incident);

        return toView(level);
    }

    @Transactional
    public void detachIncident(UUID levelId, UUID incidentId) {

        LevelEntity level = getLevelEntity(levelId);

        IncidentEntity incident = getIncident(incidentId);

        if (incident.getLevel() == null || !incident.getLevel().getId().equals(levelId)) {
            throw new IllegalArgumentException("Происшествие не привязано к уровню");
        }
        
        level.removeIncident(incident);
    }

    private LevelView toView(LevelEntity level) {
        return new LevelView(
                level.getId(),
                level.getTitle(),
                level.getDifficulty(),
                level.getIncidents().stream().map(incidentMapper::toFullView).toList()
        );
    }

    private LevelEntity getLevelEntity(UUID id) {
        return levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Уровень", id));
    }

    private IncidentEntity getIncident(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));
    }
}
