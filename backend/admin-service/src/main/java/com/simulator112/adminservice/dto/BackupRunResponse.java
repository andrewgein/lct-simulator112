package com.simulator112.adminservice.dto;

import com.simulator112.adminservice.entity.BackupRun;
import java.time.Instant;
import java.util.UUID;

public record BackupRunResponse(
    UUID id,
    Instant startedAt,
    Instant finishedAt,
    String status,
    Long sizeBytes,
    UUID triggeredBy,
    String errorMessage) {

  public static BackupRunResponse from(BackupRun run) {
    return new BackupRunResponse(
        run.getId(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getStatus(),
        run.getSizeBytes(),
        run.getTriggeredBy(),
        run.getErrorMessage());
  }
}
