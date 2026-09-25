package com.simulator112.review_service.adapter.out.messaging;

import com.simulator112.review_service.adapter.out.messaging.dto.ReviewCommentCreatedEvent;
import com.simulator112.review_service.application.port.out.ReviewCommentNotificationPort;
import com.simulator112.review_service.domain.model.ReviewComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaReviewCommentNotificationAdapter implements ReviewCommentNotificationPort {
    static final String TOPIC = "review.comment.created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(ReviewComment comment, UUID recipientUserId) {
        var event = new ReviewCommentCreatedEvent(comment.id(), comment.reviewContextId(), recipientUserId,
                comment.authorId(), comment.text(), comment.createdAt());
        kafkaTemplate.send(TOPIC, recipientUserId.toString(), event).whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("Опубликовано событие комментария: eventId={}, recipientUserId={}",
                        comment.id(), recipientUserId);
            } else {
                log.error("Не удалось опубликовать событие комментария: eventId={}", comment.id(), exception);
            }
        });
    }
}
