package com.simulator112.adminservice.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BackupRun {

  private UUID id;
  private Instant startedAt;
  private Instant finishedAt;
  private String status;
  private Long sizeBytes;
  private String objectKey;
  private UUID triggeredBy;
  private String errorMessage;

  public static BackupRun start(UUID triggeredBy) {
    BackupRun run = new BackupRun();
    run.setId(UUID.randomUUID());
    run.setStartedAt(Instant.now());
    run.setStatus("RUNNING");
    run.setTriggeredBy(triggeredBy);
    return run;
  }
}
