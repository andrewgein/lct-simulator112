package com.simulator112.notification.application.model;

import java.time.Instant;
import java.util.UUID;

public record ReviewCommentNotice(UUID eventId, UUID recipientUserId, UUID reviewContextId,
                                  String commentText, Instant createdAt) {}
