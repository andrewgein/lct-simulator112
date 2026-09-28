package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReviewSubmission(UUID contextId, UUID userId, UUID assignmentId, TargetType targetType,
                               List<IncidentScenario> incidents, List<CardRevision> cardRevisions,
                               List<IncidentRuntime> runtime, List<TranscriptPhrase> transcript,
                               Instant startedAt, Instant submittedAt,
                               Integer threshold3, Integer threshold4, Integer threshold5) {
    public ReviewSubmission(UUID contextId, UUID userId, UUID assignmentId, TargetType targetType,
                            List<IncidentScenario> incidents, List<CardRevision> cardRevisions,
                            List<IncidentRuntime> runtime, List<TranscriptPhrase> transcript,
                            Instant startedAt, Instant submittedAt) {
        this(contextId, userId, assignmentId, targetType, incidents, cardRevisions, runtime, transcript,
                startedAt, submittedAt, null, null, null);
    }

    public ReviewSubmission {
        incidents = List.copyOf(incidents);
        cardRevisions = List.copyOf(cardRevisions);
        runtime = List.copyOf(runtime);
        transcript = List.copyOf(transcript);
    }

    public enum TargetType {SYSTEM_112, DDS}

    public record IncidentScenario(String id, int order, List<StageScenario> stages,
                                   EvaluationCriteria criteria) {
        public IncidentScenario {
            stages = List.copyOf(stages);
            if (criteria == null) throw new IllegalArgumentException("Критерии оценки обязательны");
        }
    }

    public record EvaluationCriteria(List<DialogueCriterion> dialogueCriteria) {
        public EvaluationCriteria {
            dialogueCriteria = List.copyOf(dialogueCriteria);
            int budget = dialogueCriteria.stream().mapToInt(DialogueCriterion::weight).sum();
            if (budget > 40) {
                throw new IllegalArgumentException(
                        "Суммарный вес критериев оценки диалога не может превышать 40 баллов");
            }
        }
    }

    public record DialogueCriterion(String id, String name, String hypothesis, int weight) {
        public DialogueCriterion {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("ID критерия оценки диалога обязателен");
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Название критерия оценки диалога обязательно");
            if (hypothesis == null || hypothesis.isBlank()) throw new IllegalArgumentException("Гипотеза критерия оценки диалога обязательна");
            if (weight <= 0 || weight > 40) {
                throw new IllegalArgumentException("Вес критерия оценки диалога должен быть от 1 до 40 баллов");
            }
        }
    }

    public record StageScenario(String id, Integer position, List<String> classifierCodes, int victimCount,
                                String ddsStageType, List<CallScenario> calls, String expectedComment, String actualStatus) {
        public StageScenario(String id, Integer position, List<String> classifierCodes, int victimCount,
                             String ddsStageType, List<CallScenario> calls, String expectedComment) {
            this(id, position, classifierCodes, victimCount, ddsStageType, calls, expectedComment, null);
        }

        public StageScenario(String id, Integer position, List<String> classifierCodes, int victimCount,
                             String ddsStageType, List<CallScenario> calls) {
            this(id, position, classifierCodes, victimCount, ddsStageType, calls, null, null);
        }

        public StageScenario {
            classifierCodes = List.copyOf(classifierCodes);
            calls = List.copyOf(calls);
        }
    }

    public record CallScenario(String id, int position, Person person) {
    }

    public record Person(String firstName, String lastName, String middleName, String phone,
                         String contactPhone, String onScenePhone, String address, String additionalInfo) {
    }

    public record CardRevision(String revisionId, String cardId, long version, String callId,
                               String mainCardId, Person applicant, Integer victimCount, Map<String, String> additionalInfo,
                               boolean additionalInfoProvided, List<String> incidentTypes, List<String> services,
                               Instant createdAt) {
        public CardRevision {
            additionalInfo = Map.copyOf(additionalInfo);
            incidentTypes = List.copyOf(incidentTypes);
            services = List.copyOf(services);
        }
    }

    public record IncidentRuntime(String incidentId, String status, List<StageRuntime> stages,
                                  List<ReactionEvent> reactionEvents) {
        public IncidentRuntime(String incidentId, String status, List<StageRuntime> stages) {
            this(incidentId, status, stages, List.of());
        }

        public IncidentRuntime {
            stages = List.copyOf(stages);
            reactionEvents = List.copyOf(reactionEvents);
        }
    }

    public record ReactionEvent(String status, Instant changedAt, String comment) {
    }

    public record StageRuntime(String stageId, String stageType, String status,
                               Instant startedAt, Instant deadline, String comment, boolean completedCall,
                               List<String> completedCallIds) {
        public StageRuntime {
            completedCallIds = List.copyOf(completedCallIds);
        }

        public StageRuntime(String stageId, String stageType, String status, Instant startedAt, Instant deadline,
                            String comment, boolean completedCall) {
            this(stageId, stageType, status, startedAt, deadline, comment, completedCall, List.of());
        }

        public StageRuntime(String stageId, String stageType, String status, Instant startedAt, Instant deadline, String comment) {
            this(stageId, stageType, status, startedAt, deadline, comment, false, List.of());
        }

        public StageRuntime(String stageId, String stageType, String status, Instant startedAt, Instant deadline) {
            this(stageId, stageType, status, startedAt, deadline, null, false, List.of());
        }
    }

    public record TranscriptPhrase(String speaker, String text) {
    }
}
