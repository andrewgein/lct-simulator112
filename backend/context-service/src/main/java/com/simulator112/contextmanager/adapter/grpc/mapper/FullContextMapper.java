package com.simulator112.contextmanager.adapter.grpc.mapper;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import java.util.Comparator;

public final class FullContextMapper {
    private FullContextMapper() {}

    public static FullContext toProto(TrainingContext context) {
        var builder = FullContext.newBuilder().setUuid(context.getId().toString()).setUserId(context.getUserId().toString());
        if (!context.getIncidents().isEmpty()) {
            builder.setIncidentContext(IncidentContextMapper.toProto(context.getIncidents().getFirst()));
            var level = com.simulator112.incident.grpc.contract.LevelContext.newBuilder()
                    .setId(context.getLevelId().toString()).setTitle(context.getLevelTitle())
                    .setTargetType(com.simulator112.incident.grpc.contract.IncidentTargetType.valueOf(
                            "INCIDENT_TARGET_TYPE_" + context.getTargetType().name()))
                    .setExecutionMode(com.simulator112.incident.grpc.contract.ExecutionMode.valueOf(
                            "EXECUTION_MODE_" + context.getExecutionMode().name()));
            if (context.getDifficulty() != null) level.setDifficulty(com.simulator112.incident.grpc.contract.Difficulty.valueOf(
                    "DIFFICULTY_" + context.getDifficulty().name()));
            context.getIncidents().forEach(value -> level.addIncidents(IncidentContextMapper.toProto(value)));
            builder.setLevelContext(level);
        }
        builder.setLevelProgress(LevelProgressGrpcMapper.fromContext(context));
        if (context.getDialog() != null) builder.setDialogContext(DialogContextMapper.toProto(context.getDialog()));
        var revisions = context.getSolutionCards().stream().sorted(Comparator.comparing(
                SolutionCardRevision::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SolutionCardRevision::getId, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        builder.addAllSolutionContextRevisions(revisions.stream().map(SolutionContextMapper::toProto).toList());
        revisions.stream().max(Comparator.comparing(SolutionCardRevision::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .ifPresent(value -> builder.setSolutionContext(SolutionContextMapper.toProto(value)));
        return builder.build();
    }
}
