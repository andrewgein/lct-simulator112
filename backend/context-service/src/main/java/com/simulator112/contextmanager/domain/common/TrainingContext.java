package com.simulator112.contextmanager.domain.common;

import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import com.simulator112.shared.dto.Difficulty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainingContext {
    private UUID id;
    private UUID assignmentId;
    private String levelTitle;
    private Integer threshold3;
    private Integer threshold4;
    private Integer threshold5;
    private IncidentTargetType targetType;
    private Difficulty difficulty;
    private ExecutionMode executionMode;
    private UUID userId;
    private ContextStatus status;
    private UUID activeCallId;
    private DialogProgressStatus dialogStatus;
    private List<IncidentSnapshot> incidents = new ArrayList<>();
    private List<SolutionCardRevision> solutionCards = new ArrayList<>();
    private DialogTranscript dialog;
    private Instant createdAt;
    private Instant updatedAt;
}
