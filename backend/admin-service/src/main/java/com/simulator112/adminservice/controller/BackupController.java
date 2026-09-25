package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.BackupRunResponse;
import com.simulator112.adminservice.entity.BackupRun;
import com.simulator112.adminservice.repository.BackupRunRepository;
import com.simulator112.adminservice.service.BackupService;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

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
   * Streams every object from this run's MinIO prefix (all database dumps, plus the encrypted
   * secrets archive if present) as a single ZIP, built on the fly - nothing is buffered on disk
   * or fully in memory. Goes through this service rather than a presigned MinIO URL because MinIO
   * is only reachable on the internal docker network (S3_ENDPOINT is a container-name host), so a
   * browser could never resolve a presigned URL built against it.
   */
  @GetMapping("/api/v1/admin/backups/{id}/download")
  public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id) {
    BackupRun run =
        backupRunRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Backup run not found"));
    if (run.getObjectKey() == null) {
      throw new IllegalStateException("Backup run has no stored objects yet");
    }
    String prefix = run.getObjectKey();
    List<String> keys =
        s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build())
            .contents()
            .stream()
            .map(software.amazon.awssdk.services.s3.model.S3Object::key)
            .toList();
    if (keys.isEmpty()) {
      throw new IllegalStateException("Backup run has no stored objects yet");
    }

    StreamingResponseBody body =
        outputStream -> {
          try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            for (String key : keys) {
              zip.putNextEntry(new ZipEntry(key.substring(prefix.length())));
              try (InputStream s3Object =
                  s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
                s3Object.transferTo(zip);
              }
              zip.closeEntry();
            }
          }
        };

    String filename = prefix.replace("/", "") + ".zip";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(body);
  }
}
