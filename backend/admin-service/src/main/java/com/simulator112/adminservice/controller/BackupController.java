package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.BackupRunResponse;
import com.simulator112.adminservice.entity.BackupRun;
import com.simulator112.adminservice.repository.BackupRunRepository;
import com.simulator112.adminservice.service.BackupService;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@RestController
public class BackupController {

  private final BackupService backupService;
  private final BackupRunRepository backupRunRepository;
  private final S3Presigner s3Presigner;
  private final String bucket;

  public BackupController(
      BackupService backupService,
      BackupRunRepository backupRunRepository,
      S3Presigner s3Presigner,
      @org.springframework.beans.factory.annotation.Value("${s3.backup-bucket}") String bucket) {
    this.backupService = backupService;
    this.backupRunRepository = backupRunRepository;
    this.s3Presigner = s3Presigner;
    this.bucket = bucket;
  }

  @PostMapping("/api/v1/admin/backups/run")
  public BackupRunResponse run(@RequestHeader(value = "X-User-Id", required = false) UUID triggeredBy) {
    BackupRun run = backupService.startRun(triggeredBy);
    backupService.runBackupAsync(run.getId(), triggeredBy);
    return BackupRunResponse.from(run);
  }

  @GetMapping("/api/v1/admin/backups")
  public Page<BackupRunResponse> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return backupRunRepository
        .findAllByOrderByStartedAtDesc(PageRequest.of(page, size))
        .map(BackupRunResponse::from);
  }

  @GetMapping("/api/v1/admin/backups/{id}/download-url")
  public java.util.Map<String, String> downloadUrl(@PathVariable UUID id) {
    BackupRun run =
        backupRunRepository.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Backup run not found"));
    if (run.getObjectKey() == null) {
      throw new IllegalStateException("Backup run has no stored objects yet");
    }
    // objectKey is the folder prefix for this run (e.g. "backup-2026-01-01T03-00-00/"); presign
    // the first database dump as the primary download link, the rest share the same prefix.
    String key = run.getObjectKey() + "auth.sql.gz";
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
            .build();
    String url = s3Presigner.presignGetObject(presignRequest).url().toString();
    return java.util.Map.of("url", url, "prefix", run.getObjectKey());
  }
}
