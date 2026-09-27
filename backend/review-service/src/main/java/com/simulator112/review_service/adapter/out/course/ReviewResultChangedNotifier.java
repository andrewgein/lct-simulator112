package com.simulator112.review_service.adapter.out.course;

import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import com.simulator112.course.grpc.contract.ReviewResultNotification;
import com.simulator112.review_service.application.event.ReviewResultChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewResultChangedNotifier {
    private final CourseServiceGrpc.CourseServiceBlockingStub stub;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyCourse(ReviewResultChanged event) {
        try {
            stub.withDeadlineAfter(5, TimeUnit.SECONDS).notifyReviewResult(ReviewResultNotification.newBuilder()
                    .setUserId(event.userId().toString()).setAssignmentId(event.assignmentId().toString()).build());
        } catch (RuntimeException exception) {
            log.error("Не удалось обновить прохождение курса: {}", event, exception);
        }
    }
}
