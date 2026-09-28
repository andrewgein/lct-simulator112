package com.simulator112.adminservice.domain.model;

import java.time.Instant;

public record LogEntry(Instant timestamp, String line) {}
