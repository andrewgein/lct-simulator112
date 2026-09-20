package com.simulator112.incident.service.admin;

import com.simulator112.incident.client.classifier.ClassifierClient;
import com.simulator112.incident.dto.request.CreateStageRequest;
import com.simulator112.incident.dto.request.UpdateStageRequest;
import com.simulator112.incident.dto.view.StageView;
import com.simulator112.incident.exception.IncidentNotFoundException;
import com.simulator112.incident.exception.ResourceNotFoundException;
import com.simulator112.incident.mapper.IncidentMapper;
import com.simulator112.incident.mapper.embeddable.ApplicantMapper;
import com.simulator112.incident.model.entity.IncidentEntity;
import com.simulator112.incident.model.entity.StageEntity;
import com.simulator112.incident.repository.IncidentRepository;
import com.simulator112.incident.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StageAdminService {

    private final StageRepository stageRepository;
    private final IncidentRepository incidentRepository;
    private final ClassifierClient classifierClient;
    private final IncidentMapper incidentMapper;
    private final ApplicantMapper applicantMapper;

    @Transactional
    public StageView createStage(UUID incidentId, CreateStageRequest request) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        validateClassifierCode(request.classifierCode());
        StageEntity stage = StageEntity.builder()
                .title(request.title())
                .position(request.position())
                .classifierCode(request.classifierCode())
                .description(request.description())
                .victim(request.victim() == null ? null : applicantMapper.toEntity(request.victim()))
                .build();
        incident.addStage(stage);
        return incidentMapper.toStageView(stageRepository.save(stage));
    }

    @Transactional
    public StageView updateStage(UUID id, UpdateStageRequest request) {
        StageEntity stage = getStage(id);
        if (request.title() != null) {
            stage.setTitle(request.title());
        }
        if (request.position() != null) {
            stage.setPosition(request.position());
        }
        if (request.classifierCode() != null) {
            validateClassifierCode(request.classifierCode());
            stage.setClassifierCode(request.classifierCode());
        }
        if (request.description() != null) {
            stage.setDescription(request.description());
        }
        if (request.victim() != null) {
            stage.setVictim(applicantMapper.toEntity(request.victim()));
        }
        return incidentMapper.toStageView(stage);
    }

    @Transactional
    public void deleteStage(UUID id) {
        StageEntity stage = getStage(id);
        stage.getIncident().removeStage(stage);
    }

    private void validateClassifierCode(String code) {
        classifierClient.getEntry(code);
    }

    private StageEntity getStage(UUID id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Стадия", id));
    }
}
