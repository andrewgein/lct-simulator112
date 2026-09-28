package com.simulator112.contextmanager.domain.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.simulator112.shared.dto.Difficulty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncidentSnapshot {
    private UUID persistenceId;
    private UUID sourceId;
    private int position;
    private String title;
    private IncidentTargetType targetType;
    private Difficulty difficulty;
    private IncidentProgressStatus status;
    private UUID activeStageId;
    private Address address;
    private Criteria criteria;
    private List<String> preparedCardClassifierCodes = new ArrayList<>();
    private Person cardApplicant;
    private int cardVictimCount;
    private Map<String, String> preparedCardAdditionalInfo = new LinkedHashMap<>();
    private String initialAssignmentService;
    private List<ServiceReaction> serviceReactions = new ArrayList<>();
    private List<StageSnapshot> stages = new ArrayList<>();
}
