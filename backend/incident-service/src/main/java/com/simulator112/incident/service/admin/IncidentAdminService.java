package com.simulator112.incident.service.admin;

import com.simulator112.incident.dto.request.CreateIncidentRequest;
import com.simulator112.incident.dto.request.UpdateIncidentRequest;
import com.simulator112.incident.dto.view.IncidentFullView;
import com.simulator112.incident.exception.IncidentNotFoundException;
import com.simulator112.incident.mapper.IncidentMapper;
import com.simulator112.incident.model.entity.IncidentEntity;
import com.simulator112.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentAdminService {

    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;

    @Transactional(readOnly = true)
    public Page<IncidentFullView> getAllIncidents(Pageable pageable) {
        return incidentRepository.findAll(pageable).map(incidentMapper::toFullView);
    }

    @Transactional(readOnly = true)
    public IncidentFullView getFullIncidentById(UUID incidentId) {
        return incidentMapper.toFullView(getIncidentOrThrow(incidentId));
    }

    @Transactional
    public IncidentFullView createIncident(CreateIncidentRequest request) {
        IncidentEntity savedIncident = incidentRepository.save(incidentMapper.toEntity(request));
        return incidentMapper.toFullView(savedIncident);
    }

    @Transactional
    public IncidentFullView updateIncident(UUID incidentId, UpdateIncidentRequest request) {
        IncidentEntity incident = getIncidentOrThrow(incidentId);
        incidentMapper.applyPatch(request, incident);
        return incidentMapper.toFullView(incidentRepository.save(incident));
    }

    @Transactional
    public void deleteIncident(UUID incidentId) {
        incidentRepository.delete(getIncidentOrThrow(incidentId));
    }

    private IncidentEntity getIncidentOrThrow(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

}
