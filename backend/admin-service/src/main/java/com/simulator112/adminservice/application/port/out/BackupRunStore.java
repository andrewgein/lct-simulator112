package com.simulator112.adminservice.application.port.out;

import com.simulator112.adminservice.domain.model.BackupRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface BackupRunStore {
  BackupRun save(BackupRun run);

  Optional<BackupRun> findById(UUID id);

  Page<BackupRun> findAllByOrderByStartedAtDesc(int page, int size);
}
