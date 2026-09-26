package com.simulator112.adminservice.dto;

import java.time.Instant;

public record LogEntryResponse(Instant timestamp, String line) {}
