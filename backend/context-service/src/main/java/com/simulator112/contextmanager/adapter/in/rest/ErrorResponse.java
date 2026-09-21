package com.simulator112.contextmanager.adapter.in.rest;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
