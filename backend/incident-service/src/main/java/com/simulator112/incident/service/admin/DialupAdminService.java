package com.simulator112.incident.service.admin;

import com.simulator112.incident.dto.request.CreateDialupRequest;
import com.simulator112.incident.dto.request.UpdateDialupRequest;
import com.simulator112.incident.dto.view.DialupView;
import com.simulator112.incident.exception.ResourceNotFoundException;
import com.simulator112.incident.mapper.IncidentMapper;
import com.simulator112.incident.mapper.embeddable.DialupDetailsMapper;
import com.simulator112.incident.model.entity.DialupEntity;
import com.simulator112.incident.model.entity.StageEntity;
import com.simulator112.incident.repository.DialupRepository;
import com.simulator112.incident.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DialupAdminService {

    private final DialupRepository dialupRepository;
    private final StageRepository stageRepository;
    private final IncidentMapper incidentMapper;
    private final DialupDetailsMapper dialupDetailsMapper;
    private final com.simulator112.incident.mapper.embeddable.ApplicantMapper applicantMapper;

    @Transactional
    public DialupView createDialup(UUID stageId, CreateDialupRequest request) {

        StageEntity stage = getStage(stageId);

        DialupEntity dialup = DialupEntity.builder()
                .position(request.position())
                .applicant(request.applicant() == null ? null : applicantMapper.toEntity(request.applicant()))
                .dialupDetails(dialupDetailsMapper.toEntity(request.dialupDetails()))
                .build();
        stage.addDialup(dialup);

        return incidentMapper.toDialupView(dialupRepository.save(dialup));
    }

    @Transactional
    public DialupView updateDialup(UUID id, UpdateDialupRequest request) {

        DialupEntity dialup = getDialup(id);

        if (request.position() != null) {
            dialup.setPosition(request.position());
        }

        if (request.applicant() != null) {
            dialup.setApplicant(applicantMapper.toEntity(request.applicant()));
        }
        if (request.dialupDetails() != null) {
            dialup.setDialupDetails(dialupDetailsMapper.toEntity(request.dialupDetails()));
        }
        return incidentMapper.toDialupView(dialup);
    }

    @Transactional
    public void deleteDialup(UUID id) {

        DialupEntity dialup = getDialup(id);
        
        dialup.getStage().removeDialup(dialup);
    }

    private StageEntity getStage(UUID id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Стадия", id));
    }

    private DialupEntity getDialup(UUID id) {
        return dialupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Диалап", id));
    }
}
