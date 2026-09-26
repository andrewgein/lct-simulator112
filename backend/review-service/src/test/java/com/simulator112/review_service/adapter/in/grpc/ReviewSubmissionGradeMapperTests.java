package com.simulator112.review_service.adapter.in.grpc;

import com.simulator112.context.grpc.contract.AssignmentContext;
import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.context.grpc.contract.LevelProgress;
import com.simulator112.incident.grpc.contract.IncidentTargetType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSubmissionGradeMapperTests {
    @Test
    void readsSnapshotThresholdsIncludingZero() {
        var assignment = AssignmentContext.newBuilder()
                .setAssignmentId(UUID.randomUUID().toString())
                .setTargetType(IncidentTargetType.INCIDENT_TARGET_TYPE_SYSTEM_112)
                .setThreshold3(0).setThreshold4(60).setThreshold5(80).build();
        var context = FullContext.newBuilder().setUuid(UUID.randomUUID().toString())
                .setAssignmentContext(assignment).setLevelProgress(LevelProgress.getDefaultInstance()).build();

        var submission = ReviewSubmissionGrpcMapper.toDomain(context);

        assertThat(submission.threshold3()).isZero();
        assertThat(submission.threshold4()).isEqualTo(60);
        assertThat(submission.threshold5()).isEqualTo(80);
    }
}
