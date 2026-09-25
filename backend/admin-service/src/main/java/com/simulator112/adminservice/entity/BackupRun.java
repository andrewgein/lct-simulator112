package com.simulator112.adminservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "backup_run")
public class BackupRun {

  @Id private UUID id;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(nullable = false)
  private String status;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "object_key")
  private String objectKey;

  @Column(name = "triggered_by")
  private UUID triggeredBy;

  @Column(name = "error_message")
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
