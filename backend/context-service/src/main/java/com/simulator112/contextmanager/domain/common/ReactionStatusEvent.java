package com.simulator112.contextmanager.domain.common;

import java.time.Instant;

public record ReactionStatusEvent(ReactionStatus status, Instant changedAt, String comment) {
}
