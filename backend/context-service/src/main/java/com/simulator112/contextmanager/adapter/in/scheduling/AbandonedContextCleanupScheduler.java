package com.simulator112.contextmanager.adapter.in.scheduling;

import com.simulator112.contextmanager.application.port.in.CleanupAbandonedContextsUseCase;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AbandonedContextCleanupScheduler {
    private final CleanupAbandonedContextsUseCase useCase;

    @Value("${context.cleanup.abandoned-after-hours:24}")
    private long abandonedAfterHours;

    @Scheduled(fixedDelayString = "${context.cleanup.check-delay-ms:3600000}")
    public void cleanupAbandonedContexts() {
        useCase.cleanupAbandoned(Instant.now().minus(Duration.ofHours(abandonedAfterHours)));
    }
}
