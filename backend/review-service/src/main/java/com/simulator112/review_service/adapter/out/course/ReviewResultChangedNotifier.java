package com.simulator112.review_service.adapter.out.course;

import com.simulator112.course.grpc.contract.CourseServiceGrpc;
import com.simulator112.course.grpc.contract.ReviewResultNotification;
import com.simulator112.review_service.application.event.ReviewResultChanged;
import io.grpc.Context;
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
        // AFTER_COMMIT listeners run synchronously on the same thread as the inbound gRPC call,
        // so they inherit its Context. If that call is cancelled/times out, the cancellation
        // propagates to this call too, failing it instantly regardless of withDeadlineAfter.
        // Detach from it so this notification gets its own lifecycle.
        Context previous = Context.ROOT.attach();
        try {
            stub.withDeadlineAfter(5, TimeUnit.SECONDS).notifyReviewResult(ReviewResultNotification.newBuilder()
                    .setUserId(event.userId().toString()).setAssignmentId(event.assignmentId().toString()).build());
        } catch (RuntimeException exception) {
            log.error("Не удалось обновить прохождение курса: {}", event, exception);
        } finally {
            Context.ROOT.detach(previous);
        }
    }
}
