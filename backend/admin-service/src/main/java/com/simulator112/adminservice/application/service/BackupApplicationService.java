package com.simulator112.adminservice.application.service;

import com.simulator112.adminservice.application.port.in.ManageBackupsUseCase;
import com.simulator112.adminservice.application.port.out.BackupObjectStorage;
import com.simulator112.adminservice.application.port.out.BackupRunStore;
import com.simulator112.adminservice.application.port.out.DatabaseDumpPort;
import com.simulator112.adminservice.domain.model.BackupRun;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupApplicationService implements ManageBackupsUseCase {

  private final BackupRunStore backupRunStore;
  private final BackupObjectStorage backupObjectStorage;
  private final DatabaseDumpPort databaseDumpPort;

  @Override
  public BackupRun start(UUID triggeredBy) {
    backupObjectStorage.ensureBucketReady();
    BackupRun run = BackupRun.start(triggeredBy);
    backupRunStore.save(run);
    return run;
  }

  @Override
  @Async
  public void runAsync(UUID runId, UUID triggeredBy) {
    runBackup(runId);
  }

  @Override
  public void runScheduled() {
    runBackup(null);
  }

  @Override
  public Page<BackupRun> list(int page, int size) {
    return backupRunStore.findAllByOrderByStartedAtDesc(page, size);
  }

  @Override
  public BackupRun get(UUID id) {
    return backupRunStore.findById(id).orElseThrow(() -> new NoSuchElementException("Backup run not found"));
  }

  @Override
  public BackupDownload prepareDownload(UUID id) {
    BackupRun run = get(id);
    if (run.getObjectKey() == null) {
      throw new IllegalStateException("Backup run has no stored objects yet");
    }
    List<String> keys = backupObjectStorage.listKeys(run.getObjectKey());
    if (keys.isEmpty()) {
      throw new IllegalStateException("Backup run has no stored objects yet");
    }
    return new BackupDownload(run.getObjectKey(), keys);
  }

  private void runBackup(UUID existingRunId) {
    BackupRun run =
        existingRunId != null
            ? backupRunStore.findById(existingRunId).orElseGet(() -> BackupRun.start(null))
            : BackupRun.start(null);
    if (existingRunId == null) backupRunStore.save(run);

    long totalBytes = 0;
    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").format(LocalDateTime.now());
    String prefix = "backup-" + timestamp + "/";
    try {
      backupObjectStorage.ensureBucketReady();
      for (String db : databaseDumpPort.configuredDatabases()) {
        Path dump = databaseDumpPort.dumpDatabase(db.trim());
        long size = backupObjectStorage.upload(dump, prefix + db.trim() + ".sql.gz");
        totalBytes += size;
        Files.deleteIfExists(dump);
      }
      Optional<Path> archive = databaseDumpPort.encryptSecrets();
      if (archive.isPresent()) {
        totalBytes += backupObjectStorage.upload(archive.get(), prefix + "secrets.tar.gpg");
        Files.deleteIfExists(archive.get());
      }
      run.setStatus("SUCCEEDED");
      run.setSizeBytes(totalBytes);
      run.setObjectKey(prefix);
      backupObjectStorage.applyRetentionPolicy();
    } catch (Exception e) {
      log.error("Backup run {} failed", run.getId(), e);
      run.setStatus("FAILED");
      run.setErrorMessage(e.getMessage());
    } finally {
      run.setFinishedAt(Instant.now());
      backupRunStore.save(run);
    }
  }
}
