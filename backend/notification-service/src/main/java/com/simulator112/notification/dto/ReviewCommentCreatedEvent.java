package com.simulator112.notification.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCommentCreatedEvent {
    private UUID eventId;
    private UUID reviewContextId;
    private UUID recipientUserId;
    private UUID authorId;
    private String commentText;
    private Instant createdAt;
}
