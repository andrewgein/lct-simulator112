package com.simulator112.incident.adapter.out.persistence;

import com.simulator112.incident.adapter.out.persistence.repository.SpringDataIncidentRepository;
import com.simulator112.incident.application.port.out.IncidentRepository;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class IncidentPersistenceAdapter implements IncidentRepository {
    private final SpringDataIncidentRepository repository;
    private final IncidentPersistenceMapper mapper;

    @Override
    @Transactional
    public Incident save(Incident incident) {
        return mapper.toDomain(repository.save(mapper.toEntity(incident)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Incident> findById(UUID incidentId) {
        return repository.findById(incidentId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> findAvailable(IncidentTargetType targetType, Difficulty difficulty) {
        return repository.findAllByTargetTypeAndDifficulty(targetType, difficulty).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID incidentId) {
        repository.deleteById(incidentId);
    }
}
