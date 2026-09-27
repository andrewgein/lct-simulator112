package com.simulator112.contextmanager.domain.common;

import com.simulator112.contextmanager.domain.dds.DdsStageType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private Map<String, String> expectedRoutingFacts = new LinkedHashMap<>();
    private DdsStageType ddsStageType;
    private Integer timeLimitSeconds;
    private StageStatus status;
    private Instant startedAt;
    private Instant deadlineAt;
    private String description;
    private int victimCount;
    private List<CallSnapshot> calls = new ArrayList<>();
}
