package com.simulator112.contextmanager.grpc.mapper;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.contextmanager.model.entity.Context;
import com.simulator112.contextmanager.model.entity.SolutionContextEntity;
import com.simulator112.contextmanager.model.enums.SolutionContextStatus;

import java.util.Comparator;
import java.util.List;

public class FullContextMapper {
    private FullContextMapper() {}

    public static FullContext toProto(Context context) {
        FullContext.Builder builder = FullContext.newBuilder()
                .setUuid(context.getUuid().toString())
                .setUserId(context.getUserId().toString());
        if (!context.getIncidentContexts().isEmpty()) {
            builder.setIncidentContext(IncidentContextMapper.toProto(context.getIncidentContexts().get(0)));
            com.simulator112.incident.grpc.contract.LevelContext.Builder levelBuilder =
                    com.simulator112.incident.grpc.contract.LevelContext.newBuilder()
                            .setId(context.getLevelId().toString());
            if (context.getDifficulty() != null) {
                levelBuilder.setDifficulty(com.simulator112.incident.grpc.contract.Difficulty.valueOf(
                        context.getDifficulty().name()));
            }
            context.getIncidentContexts().forEach(incident ->
                    levelBuilder.addIncidents(IncidentContextMapper.toProto(incident)));
            builder.setLevelContext(levelBuilder.build());
        }
        if (context.getDialogContext() != null) {
            builder.setDialogContext(DialogContextMapper.toProto(context.getDialogContext()));
        }

        List<SolutionContextEntity> revisions = context.getSolutionContexts().stream()
                .sorted(Comparator.comparing(SolutionContextEntity::getCreatedAt)
                        .thenComparing(SolutionContextEntity::getId))
                .toList();
        builder.addAllSolutionContextRevisions(
                revisions.stream().map(SolutionContextMapper::toProto).toList());

        // Keep the old field populated until review-service switches to the revision list.
        revisions.stream()
                .filter(revision -> revision.getStatus() == SolutionContextStatus.ACTIVE)
                .max(Comparator.comparing(SolutionContextEntity::getCreatedAt))
                .ifPresent(revision -> builder.setSolutionContext(SolutionContextMapper.toProto(revision)));
        return builder.build();
    }
}
