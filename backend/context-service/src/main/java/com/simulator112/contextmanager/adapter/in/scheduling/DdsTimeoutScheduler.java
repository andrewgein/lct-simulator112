package com.simulator112.contextmanager.adapter.in.scheduling;

import com.simulator112.contextmanager.application.port.in.ProcessDdsTimeoutsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DdsTimeoutScheduler {
    private final ProcessDdsTimeoutsUseCase processDdsTimeouts;

    @Scheduled(fixedDelayString = "${context.dds.timeout-check-delay-ms:1000}")
    public void processExpiredStages() {
        processDdsTimeouts.processExpiredStages();
    }
}
