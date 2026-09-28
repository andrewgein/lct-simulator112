package com.simulator112.adminservice.application.port.in;

import com.simulator112.adminservice.domain.model.BackupRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface ManageBackupsUseCase {
  BackupRun start(UUID triggeredBy);

  void runAsync(UUID runId, UUID triggeredBy);

  void runScheduled();

  Page<BackupRun> list(int page, int size);

  BackupRun get(UUID id);

  BackupDownload prepareDownload(UUID id);

  record BackupDownload(String prefix, List<String> keys) {}
}
