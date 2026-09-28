package com.simulator112.adminservice.adapter.in.web.dto;

import java.time.Instant;

public record LogEntryResponse(Instant timestamp, String line) {}
