package com.simulator112.review_service.domain.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReviewSubmission(UUID contextId, UUID userId, UUID levelId, TargetType targetType,
                               List<IncidentScenario> incidents, List<CardRevision> cardRevisions,
                               List<IncidentRuntime> runtime) {
    public ReviewSubmission {
        incidents = List.copyOf(incidents);
        cardRevisions = List.copyOf(cardRevisions);
        runtime = List.copyOf(runtime);
    }

    public enum TargetType {SYSTEM_112, DDS}

    public record IncidentScenario(String id, int order, List<StageScenario> stages) {
        public IncidentScenario {
            stages = List.copyOf(stages);
        }
    }

    public record StageScenario(String id, Integer position, List<String> classifierCodes, Person victim,
                                String ddsStageType, List<CallScenario> calls) {
        public StageScenario {
            classifierCodes = List.copyOf(classifierCodes);
            calls = List.copyOf(calls);
        }
    }

    public record CallScenario(String id, int position, Person person) {
    }

    public record Person(String firstName, String lastName, String middleName, String phone,
                         String contactPhone, String address, String additionalInfo) {
    }

    public record CardRevision(String revisionId, String cardId, long version, String callId,
                               String mainCardId, Person applicant, Person victim, Map<String, String> additionalInfo,
                               boolean additionalInfoProvided, List<String> incidentTypes) {
        public CardRevision {
            additionalInfo = Map.copyOf(additionalInfo);
            incidentTypes = List.copyOf(incidentTypes);
        }
    }

    public record IncidentRuntime(String incidentId, String status, List<StageRuntime> stages) {
        public IncidentRuntime {
            stages = List.copyOf(stages);
        }
    }

    public record StageRuntime(String stageId, String stageType, String status) {
    }
}
