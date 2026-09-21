package com.simulator112.contextmanager.domain.common;

import java.util.UUID;

public record DialogProgress(UUID contextId, UUID activeCallId, DialogProgressStatus status) {
}
