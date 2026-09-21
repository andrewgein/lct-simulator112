package com.simulator112.contextmanager.domain.common;

import com.simulator112.contextmanager.domain.dds.DdsProgress;
import com.simulator112.contextmanager.domain.system112.System112Progress;
import java.util.UUID;

public record IncidentProgress(
        UUID incidentId,
        IncidentProgressStatus status,
        System112Progress system112,
        DdsProgress dds
) {
    public IncidentProgress {
        if ((system112 == null) == (dds == null)) {
            throw new IllegalArgumentException("Прогресс инцидента должен содержать ровно один тип деталей");
        }
    }
}
