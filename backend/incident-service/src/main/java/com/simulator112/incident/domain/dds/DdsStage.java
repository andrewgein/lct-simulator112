package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.CallScenario;
import com.simulator112.incident.domain.common.IncidentStage;
import com.simulator112.incident.domain.common.IncidentStatus;

import java.util.List;
import java.util.UUID;

public record DdsStage(
        UUID id,
        String title,
        String description,
        DdsStageType type,
        int timeLimitSeconds,
        List<CallScenario> calls,
        String expectedComment,
        IncidentStatus actualStatus) implements IncidentStage {
    public DdsStage(UUID id, String title, String description, DdsStageType type,
                    int timeLimitSeconds, List<CallScenario> calls, String expectedComment) {
        this(id, title, description, type, timeLimitSeconds, calls, expectedComment, null);
    }

    public DdsStage(UUID id, String title, String description, DdsStageType type,
                    int timeLimitSeconds, List<CallScenario> calls) {
        this(id, title, description, type, timeLimitSeconds, calls, null, null);
    }

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
