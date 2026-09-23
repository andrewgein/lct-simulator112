package com.simulator112.contextmanager.adapter.out.grpc;

import com.simulator112.contextmanager.application.port.out.CourseAssignmentPort;
import com.simulator112.contextmanager.application.port.out.IncidentCatalogPort;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.AssignmentScenario;
import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import com.simulator112.course.grpc.contract.GetAssignmentForUserRequest;
import com.simulator112.shared.dto.Difficulty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAssignmentGrpcClient implements CourseAssignmentPort {
    private final CourseServiceGrpc.CourseServiceBlockingStub stub;
    private final IncidentCatalogPort incidents;

    @Override
    public AssignmentScenario getAssignmentForUser(UUID assignmentId, UUID userId) {
        try {
            var response = stub.withDeadlineAfter(3, TimeUnit.SECONDS).getAssignmentForUser(
                    GetAssignmentForUserRequest.newBuilder()
                            .setUserId(userId.toString())
                            .setAssignmentId(assignmentId.toString())
                            .build());
            if (!response.hasAssignment() || !assignmentId.toString().equals(response.getAssignment().getId())) {
                throw new IllegalArgumentException("Задание недоступно");
            }
            var assignment = response.getAssignment();
            var snapshots = IntStream.range(0, assignment.getIncidentIdsCount())
                    .mapToObj(index -> incidents.getIncident(
                            UUID.fromString(assignment.getIncidentIds(index)), index)).toList();
            if (snapshots.isEmpty()) throw new IllegalArgumentException("В задании нет инцидентов");
            var targetType = IncidentTargetType.valueOf(response.getCourseTargetType().name()
                    .replace("COURSE_TARGET_TYPE_", ""));
            var difficulty = Difficulty.valueOf(assignment.getDifficulty().name()
                    .replace("ASSIGNMENT_DIFFICULTY_", ""));
            if (snapshots.stream().anyMatch(value -> value.getTargetType() != targetType)) {
                throw new IllegalArgumentException("Инциденты не соответствуют профилю задания");
            }
            return new AssignmentScenario(assignmentId, userId, assignment.getTitle(), targetType, difficulty,
                    ExecutionMode.valueOf(assignment.getExecutionMode().name()
                            .replace("ASSIGNMENT_EXECUTION_MODE_", "")), snapshots);
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() == Status.Code.PERMISSION_DENIED
                    || exception.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new IllegalArgumentException("Курс не назначен пользователю или задание недоступно", exception);
            }
            throw exception;
        }
    }
}
