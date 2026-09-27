package com.simulator112.review_service.adapter.out.course;

import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import com.simulator112.course.grpc.contract.TeacherStudentRequest;
import com.simulator112.review_service.application.port.out.TeacherStudentAccessPort;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseServiceTeacherStudentAccessAdapter implements TeacherStudentAccessPort {
    private final CourseServiceGrpc.CourseServiceBlockingStub stub;

    @Override
    public boolean isStudentOfTeacher(UUID teacherId, UUID studentId) {
        try {
            return stub.withDeadlineAfter(2, TimeUnit.SECONDS).isStudentOfTeacher(TeacherStudentRequest.newBuilder()
                    .setTeacherId(teacherId.toString()).setStudentId(studentId.toString()).build())
                    .getBelongsToTeacher();
        } catch (StatusRuntimeException exception) {
            log.error("Не удалось проверить принадлежность ученика {} преподавателю {}", studentId, teacherId, exception);
            return false;
        }
    }
}
