package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.CallScenario;
import com.simulator112.incident.domain.common.IncidentStage;

import java.util.List;
import java.util.UUID;

public record DdsStage(
        UUID id,
        String title,
        String description,
        DdsStageType type,
        int timeLimitSeconds,
        List<CallScenario> calls) implements IncidentStage {
    public DdsStage {
        calls = calls == null ? List.of() : List.copyOf(calls);
        if (type == null) {
            throw new IllegalArgumentException("Тип этапа ДДС обязателен");
        }
        if (timeLimitSeconds <= 0) {
            throw new IllegalArgumentException("Ограничение времени этапа ДДС должно быть положительным");
        }
    }
}
