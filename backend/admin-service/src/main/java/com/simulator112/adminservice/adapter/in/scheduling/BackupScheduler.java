package com.simulator112.adminservice.adapter.in.scheduling;

import com.simulator112.adminservice.application.port.in.ManageBackupsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BackupScheduler {

  private final ManageBackupsUseCase manageBackups;

  @Scheduled(cron = "${backup.cron}")
  public void runScheduledBackup() {
    manageBackups.runScheduled();
  }
}
