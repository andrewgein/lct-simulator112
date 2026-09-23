package com.simulator112.review_service.adapter.in.grpc;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.context.grpc.contract.SolutionContext;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.IncidentStage;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.UUID;
import java.util.stream.IntStream;

final class ReviewSubmissionGrpcMapper {
    private ReviewSubmissionGrpcMapper() {
    }

    static ReviewSubmission toDomain(FullContext source) {
        if (!source.hasAssignmentContext() || !source.hasLevelProgress()) {
            throw new IllegalArgumentException("Для проверки требуется снимок задания и runtime-прогресс");
        }
        var assignment = source.getAssignmentContext();
        var target = ReviewSubmission.TargetType.valueOf(
                assignment.getTargetType().name().replace("INCIDENT_TARGET_TYPE_", ""));
        var incidents = IntStream.range(0, assignment.getIncidentsCount())
                .mapToObj(index -> incident(assignment.getIncidents(index), index + 1)).toList();
        var runtime = source.getLevelProgress().getIncidentsList().stream().map(value ->
                        new ReviewSubmission.IncidentRuntime(value.getIncidentId(),
                                value.getStatus().name().replace("INCIDENT_PROGRESS_STATUS_", ""),
                                value.hasDds() ? value.getDds().getStagesList().stream()
                                        .map(stage -> new ReviewSubmission.StageRuntime(stage.getStageId(),
                                                stage.getStageType(), stage.getStatus())).toList() : java.util.List.of()))
                .toList();
        return new ReviewSubmission(uuid(source.getUuid()), optionalUuid(source.getUserId()),
                uuid(assignment.getAssignmentId()), target, incidents, source.getSolutionContextRevisionsList().stream()
                .map(ReviewSubmissionGrpcMapper::card).toList(), runtime);
    }

    private static ReviewSubmission.IncidentScenario incident(IncidentContext source, int order) {
        return new ReviewSubmission.IncidentScenario(source.getId(), order,
                source.getStagesList().stream().map(ReviewSubmissionGrpcMapper::stage).toList());
    }

    private static ReviewSubmission.StageScenario stage(IncidentStage source) {
        return new ReviewSubmission.StageScenario(source.getId(), source.hasSystem112() ? source.getSystem112().getPosition() : null,
                source.hasSystem112() ? source.getSystem112().getClassifierCodesList() : java.util.List.of(),
                source.hasSystem112() ? source.getSystem112().getVictimCount() : 0,
                source.hasDds() ? source.getDds().getType().name().replace("DDS_STAGE_TYPE_", "") : null,
                source.getCallsList().stream().map(call -> new ReviewSubmission.CallScenario(
                        call.getId(), call.getPosition(), person(call.getPerson()))).toList());
    }

    private static ReviewSubmission.CardRevision card(SolutionContext source) {
        return new ReviewSubmission.CardRevision(source.getRevisionId(), source.getCardId(), source.getVersion(),
                source.getCallId(), source.getMainCardId(), person(source.getApplicant()),
                source.hasVictimCount() ? source.getVictimCount() : null, source.getAdditionalInfoMap(),
                source.hasAdditionalInfoProvided() ? source.getAdditionalInfoProvided() : source.getAdditionalInfoCount() > 0,
                source.getIncidentTypesList());
    }

    private static ReviewSubmission.Person person(com.simulator112.incident.grpc.contract.Person source) {
        if (source.equals(com.simulator112.incident.grpc.contract.Person.getDefaultInstance())) return null;
        return new ReviewSubmission.Person(source.getFirstName(), source.getLastName(), source.getMiddleName(),
                source.getPhone(), source.getContactPhone(), source.getOnScenePhone(), source.getAddress(),
                source.getAdditionalInfo());
    }

    private static ReviewSubmission.Person person(com.simulator112.context.grpc.contract.PersonInfo source) {
        if (source.equals(com.simulator112.context.grpc.contract.PersonInfo.getDefaultInstance())) return null;
        return new ReviewSubmission.Person(source.getFirstName(), source.getLastName(), source.getMiddleName(),
                source.getPhone(), source.getContactPhone(), source.getOnScenePhone(), source.getAddress(),
                source.getAdditionalInfo());
    }

    private static UUID uuid(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Обязательный UUID отсутствует");
        return UUID.fromString(value);
    }

    private static UUID optionalUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
