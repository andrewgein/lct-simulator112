package com.simulator112.contextmanager.adapter.grpc.mapper;

import com.simulator112.context.grpc.contract.ContextProgressStatus;
import com.simulator112.context.grpc.contract.DdsStageProgress;
import com.simulator112.context.grpc.contract.IncidentProgress;
import com.simulator112.context.grpc.contract.IncidentProgressStatus;
import com.simulator112.context.grpc.contract.LevelProgress;
import com.simulator112.context.grpc.contract.System112Progress;

public final class LevelProgressGrpcMapper {
    private LevelProgressGrpcMapper() {}

    public static LevelProgress fromContext(com.simulator112.contextmanager.domain.common.TrainingContext context) {
        var incidents = context.getIncidents().stream().map(incident -> {
            if (context.getTargetType() == com.simulator112.contextmanager.domain.common.IncidentTargetType.DDS) {
                var active = incident.getStages().stream()
                        .filter(stage -> stage.getSourceId().equals(incident.getActiveStageId())).findFirst().orElse(null);
                return new com.simulator112.contextmanager.domain.common.IncidentProgress(
                        incident.getSourceId(), incident.getStatus(), incident.getServiceReactions().stream()
                                .map(com.simulator112.contextmanager.domain.common.ServiceReactionProgress::from).toList(), null,
                        new com.simulator112.contextmanager.domain.dds.DdsProgress(incident.getActiveStageId(),
                                active == null ? null : active.getDeadlineAt(), incident.getStages().stream()
                                .map(stage -> new com.simulator112.contextmanager.domain.dds.DdsStageProgress(
                                        stage.getSourceId(), stage.getDdsStageType(), stage.getStatus(),
                                        stage.getStartedAt(), stage.getDeadlineAt())).toList()));
            }
            var calls = incident.getStages().stream().flatMap(stage -> stage.getCalls().stream()).toList();
            var activeCall = calls.stream().filter(call -> call.getStatus() == com.simulator112.contextmanager.domain.common.CallStatus.ACTIVE
                            || call.getStatus() == com.simulator112.contextmanager.domain.common.CallStatus.DISCONNECTED)
                    .map(com.simulator112.contextmanager.domain.common.CallSnapshot::getSourceId).findFirst().orElse(null);
            int completed = (int) calls.stream().filter(call -> call.getStatus() == com.simulator112.contextmanager.domain.common.CallStatus.COMPLETED).count();
            return new com.simulator112.contextmanager.domain.common.IncidentProgress(
                    incident.getSourceId(), incident.getStatus(), incident.getServiceReactions().stream()
                            .map(com.simulator112.contextmanager.domain.common.ServiceReactionProgress::from).toList(),
                    new com.simulator112.contextmanager.domain.system112.System112Progress(activeCall, completed, calls.size()), null);
        }).toList();
        return toProto(new com.simulator112.contextmanager.domain.common.LevelProgress(context.getId(), context.getTargetType(),
                context.getExecutionMode(), context.getStatus(), incidents));
    }

    public static LevelProgress toProto(com.simulator112.contextmanager.domain.common.LevelProgress progress) {
        var builder = LevelProgress.newBuilder().setContextId(progress.contextId().toString())
                .setTargetType(com.simulator112.incident.grpc.contract.IncidentTargetType.valueOf(
                        "INCIDENT_TARGET_TYPE_" + progress.targetType().name()))
                .setExecutionMode(com.simulator112.incident.grpc.contract.ExecutionMode.valueOf(
                        "EXECUTION_MODE_" + progress.executionMode().name()))
                .setStatus(ContextProgressStatus.valueOf("CONTEXT_PROGRESS_STATUS_" + progress.status().name()));
        progress.incidents().forEach(incident -> {
            var value = IncidentProgress.newBuilder().setIncidentId(incident.incidentId().toString())
                    .setStatus(IncidentProgressStatus.valueOf("INCIDENT_PROGRESS_STATUS_" + incident.status().name()));
            if (incident.system112() != null) {
                value.setSystem112(System112Progress.newBuilder()
                        .setActiveCallId(incident.system112().activeCallId() == null ? "" : incident.system112().activeCallId().toString())
                        .setCompletedCalls(incident.system112().completedCalls()).setTotalCalls(incident.system112().totalCalls()));
            } else {
                var dds = com.simulator112.context.grpc.contract.DdsProgress.newBuilder()
                        .setActiveStageId(incident.dds().activeStageId() == null ? "" : incident.dds().activeStageId().toString())
                        .setDeadline(incident.dds().deadline() == null ? "" : incident.dds().deadline().toString());
                incident.dds().stages().forEach(stage -> dds.addStages(DdsStageProgress.newBuilder()
                        .setStageId(stage.stageId().toString()).setStageType(stage.type().name())
                        .setStatus(stage.status().name())
                        .setStartedAt(stage.startedAt() == null ? "" : stage.startedAt().toString())
                        .setDeadline(stage.deadline() == null ? "" : stage.deadline().toString())));
                value.setDds(dds);
            }
            builder.addIncidents(value);
        });
        return builder.build();
    }
}
