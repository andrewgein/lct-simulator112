package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.BackupRunResponse;
import com.simulator112.adminservice.entity.BackupRun;
import com.simulator112.adminservice.repository.BackupRunRepository;
import com.simulator112.adminservice.service.BackupService;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@RestController
public class BackupController {

  private final BackupService backupService;
  private final BackupRunRepository backupRunRepository;
  private final S3Client s3Client;
  private final String bucket;

  public BackupController(
      BackupService backupService,
      BackupRunRepository backupRunRepository,
      S3Client s3Client,
      @Value("${s3.backup-bucket}") String bucket) {
    this.backupService = backupService;
    this.backupRunRepository = backupRunRepository;
    this.s3Client = s3Client;
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

  /**
   * Streams the backup file through this service rather than handing back a presigned MinIO URL:
   * MinIO is only reachable on the internal docker network (S3_ENDPOINT is a container-name
   * host), so a browser could never resolve a presigned URL built against it. admin-service is
   * already publicly reachable through the gateway, so it fetches the object itself and passes
   * the bytes straight through.
   */
  @GetMapping("/api/v1/admin/backups/{id}/download")
  public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
    BackupRun run =
        backupRunRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Backup run not found"));
    if (run.getObjectKey() == null) {
      throw new IllegalStateException("Backup run has no stored objects yet");
    }
    // objectKey is the folder prefix for this run (e.g. "backup-2026-01-01T03-00-00/"); the auth
    // database dump is the primary download - the rest of the run's objects share the same prefix
    // and can be fetched from MinIO directly by an operator if needed.
    String key = run.getObjectKey() + "auth.sql.gz";
    var s3Object = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    String filename = run.getObjectKey().replace("/", "") + "-auth.sql.gz";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new InputStreamResource(s3Object));
  }
}
