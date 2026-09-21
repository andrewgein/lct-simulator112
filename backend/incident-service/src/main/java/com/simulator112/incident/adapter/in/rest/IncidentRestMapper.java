package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.dds.DdsCriteria;
import com.simulator112.incident.domain.dds.DdsIncident;
import com.simulator112.incident.domain.dds.DdsStage;
import com.simulator112.incident.domain.system112.System112Criteria;
import com.simulator112.incident.domain.system112.System112Incident;
import com.simulator112.incident.domain.system112.System112Stage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class IncidentRestMapper {
    public Incident toDomain(UUID id, IncidentRequest request) {
        List<String> mistakes = request.criticalMistakes() == null ? List.of() : request.criticalMistakes();
        if (request.targetType() == IncidentTargetType.DDS) {
            var stages = request.stages().stream()
                    .map(stage -> new DdsStage(stage.id(), stage.title(), stage.description(), stage.type(),
                            require(stage.timeLimitSeconds(), "Ограничение времени этапа ДДС обязательно"),
                            stage.calls()))
                    .toList();
            return new DdsIncident(id, request.title(), request.address(), request.difficulty(), stages,
                    request.preparedCardTemplate(), request.initialAssignment(),
                    new DdsCriteria(request.requiredQuestions(), request.expectedActions(), mistakes),
                    request.initialStageId(), request.transitions());
        }
        var stages = request.stages().stream()
                .map(stage -> new System112Stage(stage.id(), stage.title(),
                        require(stage.position(), "Позиция этапа системы 112 обязательна"),
                        stage.classifierCode(), stage.victim(), stage.description(), stage.calls()))
                .toList();
        return new System112Incident(id, request.title(), request.address(), request.difficulty(), stages,
                new System112Criteria(request.requiredQuestions(), request.expectedActions(), mistakes));
    }

    private int require(Integer value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
