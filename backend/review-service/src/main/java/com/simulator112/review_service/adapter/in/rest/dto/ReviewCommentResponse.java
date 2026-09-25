package com.simulator112.review_service.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewCommentResponse(UUID id, UUID reviewContextId, UUID authorId, String text, Instant createdAt) {
}
