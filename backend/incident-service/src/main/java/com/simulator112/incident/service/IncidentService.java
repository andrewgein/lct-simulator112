package com.simulator112.incident.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simulator112.incident.dto.view.IncidentPreView;
import com.simulator112.incident.exception.IncidentNotFoundException;
import com.simulator112.incident.mapper.IncidentMapper;
import com.simulator112.incident.model.entity.IncidentEntity;
import com.simulator112.incident.repository.IncidentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;

    @Transactional(readOnly = true)
    public IncidentPreView getPreviewIncidentById(UUID incidentId) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        return incidentMapper.toPreView(incident);
    }
}
