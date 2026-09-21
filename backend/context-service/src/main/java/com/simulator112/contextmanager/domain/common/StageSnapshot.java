package com.simulator112.contextmanager.domain.common;

import com.simulator112.contextmanager.domain.dds.DdsStageType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StageSnapshot {
    private UUID persistenceId;
    private UUID sourceId;
    private Integer position;
    private String title;
    private List<String> classifierCodes = new ArrayList<>();
    private DdsStageType ddsStageType;
    private Integer timeLimitSeconds;
    private StageStatus status;
    private Instant startedAt;
    private Instant deadlineAt;
    private String description;
    private Person victim;
    private List<CallSnapshot> calls = new ArrayList<>();
}
