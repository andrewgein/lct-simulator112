package com.simulator112.review_service.domain.model;

import java.time.Instant;

public record CallRecording(String callId, String fileName, Instant startedAt) {
}
