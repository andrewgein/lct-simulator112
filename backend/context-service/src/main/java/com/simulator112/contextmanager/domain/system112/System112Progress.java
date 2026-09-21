package com.simulator112.contextmanager.domain.system112;

import java.util.UUID;

public record System112Progress(UUID activeCallId, int completedCalls, int totalCalls) {
    public System112Progress {
        if (completedCalls < 0 || totalCalls < 0 || completedCalls > totalCalls) {
            throw new IllegalArgumentException("Некорректный прогресс звонков системы 112");
        }
    }
}
