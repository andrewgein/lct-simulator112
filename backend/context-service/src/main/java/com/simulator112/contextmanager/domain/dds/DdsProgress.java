package com.simulator112.contextmanager.domain.dds;

import java.time.Instant;
import java.util.UUID;

public record DdsProgress(UUID activeStageId, Instant deadline) {
}
