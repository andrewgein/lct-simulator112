package com.simulator112.incident.service.admin;

import com.simulator112.incident.dto.request.CreateStageRequest;
import com.simulator112.incident.dto.request.IncidentAdditionalInfoRequest;
import com.simulator112.incident.dto.request.UpdateStageRequest;
import com.simulator112.incident.dto.view.StageView;
import com.simulator112.incident.exception.IncidentNotFoundException;
import com.simulator112.incident.exception.ResourceNotFoundException;
import com.simulator112.incident.exception.TypeNotFoundException;
import com.simulator112.incident.mapper.IncidentMapper;
import com.simulator112.incident.mapper.embeddable.ApplicantMapper;
import com.simulator112.incident.model.entity.AdditionalInfoEntity;
import com.simulator112.incident.model.entity.IncidentEntity;
import com.simulator112.incident.model.entity.StageAdditionalInfoEntity;
import com.simulator112.incident.model.entity.StageEntity;
import com.simulator112.incident.model.entity.TypeEntity;
import com.simulator112.incident.repository.AdditionalInfoRepository;
import com.simulator112.incident.repository.IncidentRepository;
import com.simulator112.incident.repository.StageRepository;
import com.simulator112.incident.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StageAdminService {

    private final StageRepository stageRepository;
    private final IncidentRepository incidentRepository;
    private final TypeRepository typeRepository;
    private final AdditionalInfoRepository additionalInfoRepository;
    private final IncidentMapper incidentMapper;
    private final ApplicantMapper applicantMapper;

    @Transactional
    public StageView createStage(UUID incidentId, CreateStageRequest request) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        TypeEntity type = getType(request.typeId());
        StageEntity stage = StageEntity.builder()
                .title(request.title())
                .position(request.position())
                .type(type)
                .description(request.description())
                .victim(request.victim() == null ? null : applicantMapper.toEntity(request.victim()))
                .build();
        applyAdditionalInfo(stage, type, request.additionalInfo());
        incident.addStage(stage);
        return incidentMapper.toStageView(stageRepository.save(stage));
    }

    @Transactional
    public StageView updateStage(UUID id, UpdateStageRequest request) {
        StageEntity stage = getStage(id);
        TypeEntity type = request.typeId() == null ? stage.getType() : getType(request.typeId());
        if (request.title() != null) {
            stage.setTitle(request.title());
        }
        if (request.position() != null) {
            stage.setPosition(request.position());
        }
        if (request.typeId() != null) {
            stage.setType(type);
        }
        if (request.description() != null) {
            stage.setDescription(request.description());
        }
        if (request.victim() != null) {
            stage.setVictim(applicantMapper.toEntity(request.victim()));
        }
        if (request.additionalInfo() != null) {
            updateAdditionalInfo(stage, type, request.additionalInfo());
        }
        return incidentMapper.toStageView(stage);
    }

    @Transactional
    public void deleteStage(UUID id) {
        StageEntity stage = getStage(id);
        stage.getIncident().removeStage(stage);
    }

    private void applyAdditionalInfo(StageEntity stage, TypeEntity type, List<IncidentAdditionalInfoRequest> values) {
        Map<UUID, AdditionalInfoEntity> fields = getAdditionalInfoFields(type, values);
        values.forEach(value -> stage.addAdditionalInfo(StageAdditionalInfoEntity.builder()
                .additionalInfo(fields.get(value.additionalInfoId()))
                .fieldValue(value.fieldValue())
                .build()));
    }

    private void updateAdditionalInfo(StageEntity stage, TypeEntity type, List<IncidentAdditionalInfoRequest> values) {
        Map<UUID, AdditionalInfoEntity> fields = getAdditionalInfoFields(type, values);
        Map<UUID, StageAdditionalInfoEntity> existing = stage.getAdditionalInfo().stream()
                .collect(Collectors.toMap(value -> value.getAdditionalInfo().getId(), Function.identity()));
        stage.getAdditionalInfo().removeIf(value -> !fields.containsKey(value.getAdditionalInfo().getId()));
        values.forEach(value -> {
            StageAdditionalInfoEntity current = existing.get(value.additionalInfoId());
            if (current == null) {
                stage.addAdditionalInfo(StageAdditionalInfoEntity.builder()
                        .additionalInfo(fields.get(value.additionalInfoId()))
                        .fieldValue(value.fieldValue())
                        .build());
            } else {
                current.setFieldValue(value.fieldValue());
            }
        });
    }

    private Map<UUID, AdditionalInfoEntity> getAdditionalInfoFields(TypeEntity type, List<IncidentAdditionalInfoRequest> values) {
        List<UUID> ids = values.stream().map(IncidentAdditionalInfoRequest::additionalInfoId).toList();
        if (ids.stream().distinct().count() != ids.size()) {
            throw new IllegalArgumentException("Дополнительное поле этапа указано несколько раз");
        }
        Map<UUID, AdditionalInfoEntity> fields = additionalInfoRepository.findAllById(ids).stream()
                .filter(field -> field.getType().getId().equals(type.getId()))
                .collect(Collectors.toMap(AdditionalInfoEntity::getId, Function.identity()));
        ids.stream().filter(id -> !fields.containsKey(id)).findFirst().ifPresent(id -> {
            throw new ResourceNotFoundException("Дополнительное поле типа этапа", id);
        });
        type.getFields().stream()
                .filter(AdditionalInfoEntity::isRequired)
                .filter(field -> !ids.contains(field.getId()))
                .findFirst()
                .ifPresent(field -> {
                    throw new IllegalArgumentException("Обязательное поле не заполнено: " + field.getFieldName());
                });
        return fields;
    }

    private TypeEntity getType(UUID id) {
        return typeRepository.findById(id)
                .orElseThrow(() -> new TypeNotFoundException(id));
    }

    private StageEntity getStage(UUID id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Стадия", id));
    }
}
