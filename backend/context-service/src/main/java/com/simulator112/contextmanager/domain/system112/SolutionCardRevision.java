package com.simulator112.contextmanager.domain.system112;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolutionCardRevision {
    private UUID id;
    private UUID contextId;
    private UUID cardId;
    private UUID previousRevisionId;
    private long version;
    private SolutionContextStatus status;
    private UUID callId;
    private UUID parentCardId;
    private UUID duplicateOfCardId;
    private PersonInfo applicant;
    private PersonInfo victim;
    private Map<String, String> additionalInfo = new HashMap<>();
    private boolean additionalInfoProvided;
    private String incidentType;
    private Instant createdAt;
}
