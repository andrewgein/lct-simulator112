package com.simulator112.review_service.adapter.in.rest.dto;

import java.time.Instant;

public record CallRecordingResponse(String callId, String fileName, Instant startedAt) {
}
