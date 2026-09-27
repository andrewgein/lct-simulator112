package com.simulator112.review_service.application.event;

import java.util.UUID;

public record ReviewResultChanged(UUID userId, UUID assignmentId) {}
