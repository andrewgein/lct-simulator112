package com.simulator112.review_service.adapter.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewCommentCreatedEvent(UUID eventId, UUID reviewContextId, UUID recipientUserId,
                                        UUID authorId, String commentText, Instant createdAt) {
}
