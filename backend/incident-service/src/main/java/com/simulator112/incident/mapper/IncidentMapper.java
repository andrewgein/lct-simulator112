package com.simulator112.incident.mapper;

import com.simulator112.incident.client.classifier.ClassifierClient;
import com.simulator112.incident.dto.request.CreateIncidentRequest;
import com.simulator112.incident.dto.request.UpdateIncidentRequest;
import com.simulator112.incident.dto.view.DialupView;
import com.simulator112.incident.dto.view.IncidentFullView;
import com.simulator112.incident.dto.view.IncidentPreView;
import com.simulator112.incident.dto.view.StageView;
import com.simulator112.incident.mapper.embeddable.AddressMapper;
import com.simulator112.incident.mapper.embeddable.ApplicantMapper;
import com.simulator112.incident.mapper.embeddable.DialupDetailsMapper;
import com.simulator112.incident.mapper.embeddable.DispatcherCriteriaMapper;
import com.simulator112.incident.model.entity.DialupEntity;
import com.simulator112.incident.model.entity.IncidentEntity;
import com.simulator112.incident.model.entity.StageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncidentMapper {

    private final AddressMapper addressMapper;
    private final ApplicantMapper applicantMapper;
    private final DialupDetailsMapper dialupDetailsMapper;
    private final DispatcherCriteriaMapper dispatcherCriteriaMapper;
    private final ClassifierClient classifierClient;

    public IncidentEntity toEntity(CreateIncidentRequest request) {
        IncidentEntity entity = new IncidentEntity();
        entity.setTitle(request.title());
        entity.setAddress(addressMapper.toEntity(request.address()));
        entity.setCriteria(dispatcherCriteriaMapper.toEntity(request.criteria()));
        return entity;
    }

    public void applyPatch(UpdateIncidentRequest request, IncidentEntity entity) {
        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        if (request.address() != null) {
            entity.setAddress(addressMapper.toEntity(request.address()));
        }
        if (request.criteria() != null) {
            entity.setCriteria(dispatcherCriteriaMapper.toEntity(request.criteria()));
        }
    }

    public IncidentFullView toFullView(IncidentEntity entity) {
        return new IncidentFullView(
                entity.getId(),
                entity.getTitle(),
                addressMapper.toView(entity.getAddress()),
                dispatcherCriteriaMapper.toView(entity.getCriteria()),
                entity.getLevel() == null ? null : entity.getLevel().getId(),
                entity.getStages().stream().map(this::toStageView).toList()
        );
    }

    public StageView toStageView(StageEntity stage) {
        return new StageView(
                stage.getId(),
                stage.getTitle(),
                stage.getPosition(),
                classifierClient.getEntry(stage.getClassifierCode()),
                stage.getDescription(),
                stage.getVictim() == null ? null : applicantMapper.toView(stage.getVictim()),
                stage.getDialups().stream().map(this::toDialupView).toList()
        );
    }

    public DialupView toDialupView(DialupEntity dialup) {
        return new DialupView(
                dialup.getId(),
                dialup.getPosition(),
                dialup.getApplicant() == null ? null : applicantMapper.toView(dialup.getApplicant()),
                dialupDetailsMapper.toView(dialup.getDialupDetails())
        );
    }

    public IncidentPreView toPreView(IncidentEntity entity) {
        return new IncidentPreView(entity.getId(), entity.getTitle());
    }
}
