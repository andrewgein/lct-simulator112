package com.simulator112.contextmanager.domain.common;

import com.simulator112.contextmanager.domain.dds.DdsStageDetails;
import com.simulator112.contextmanager.domain.system112.System112StageDetails;
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
    private System112StageDetails system112;
    private DdsStageDetails dds;
    private StageStatus status;
    private Instant startedAt;
    private Instant deadlineAt;
    private String description;
    private List<CallSnapshot> calls = new ArrayList<>();
}
