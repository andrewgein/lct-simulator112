package com.simulator112.adminservice.service;

import com.simulator112.adminservice.entity.BackupRun;
import com.simulator112.adminservice.repository.BackupRunRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Dumps every service's logical Postgres database with pg_dump, gzips each dump, and uploads the
 * result to the MinIO "backups" bucket. Runs on a daily schedule and can also be triggered
 * on-demand from the admin UI. No separate backup container: admin-service's own image ships the
 * postgresql-client binary (see Dockerfile) purely so ProcessBuilder can shell out to pg_dump.
 */
@Slf4j
@Service
public class BackupService {

  private final BackupRunRepository backupRunRepository;
  private final S3Client s3Client;

  private final String dbHost;
  private final List<String> databases;
  private final String dbUsername;
  private final String dbPassword;
  private final String bucket;
  private final int retentionDays;
  private final String secretsDir;
  private final String encryptionKey;

  public BackupService(
      BackupRunRepository backupRunRepository,
      S3Client s3Client,
      @Value("${backup.db-host}") String dbHost,
      @Value("${backup.db-databases}") String databasesCsv,
      @Value("${backup.db-username}") String dbUsername,
      @Value("${backup.db-password}") String dbPassword,
      @Value("${s3.backup-bucket}") String bucket,
      @Value("${backup.retention-days}") int retentionDays,
      @Value("${backup.secrets-dir}") String secretsDir,
      @Value("${backup.encryption-key}") String encryptionKey) {
    this.backupRunRepository = backupRunRepository;
    this.s3Client = s3Client;
    this.dbHost = dbHost;
    this.databases = List.of(databasesCsv.split(","));
    this.dbUsername = dbUsername;
    this.dbPassword = dbPassword;
    this.bucket = bucket;
    this.retentionDays = retentionDays;
    this.secretsDir = secretsDir;
    this.encryptionKey = encryptionKey;
  }

  @Scheduled(cron = "${backup.cron}")
  public void runScheduledBackup() {
    runBackup(null);
  }

  @Async
  public void runBackupAsync(UUID runId, UUID triggeredBy) {
    runBackup(runId);
  }

  public BackupRun startRun(UUID triggeredBy) {
    ensureBucketExists();
    BackupRun run = BackupRun.start(triggeredBy);
    backupRunRepository.save(run);
    return run;
  }

  private void runBackup(UUID existingRunId) {
    BackupRun run =
        existingRunId != null
            ? backupRunRepository.findById(existingRunId).orElseGet(() -> BackupRun.start(null))
            : BackupRun.start(null);
    if (existingRunId == null) backupRunRepository.save(run);

    long totalBytes = 0;
    String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").format(java.time.LocalDateTime.now());
    String prefix = "backup-" + timestamp + "/";
    try {
      ensureBucketExists();
      for (String db : databases) {
        Path dump = dumpDatabase(db.trim());
        long size = uploadToS3(dump, prefix + db.trim() + ".sql.gz");
        totalBytes += size;
        Files.deleteIfExists(dump);
      }
      if (secretsDir != null && !secretsDir.isBlank() && encryptionKey != null && !encryptionKey.isBlank()) {
        Path archive = encryptSecrets();
        if (archive != null) {
          totalBytes += uploadToS3(archive, prefix + "secrets.tar.gpg");
          Files.deleteIfExists(archive);
        }
      }
      run.setStatus("SUCCEEDED");
      run.setSizeBytes(totalBytes);
      run.setObjectKey(prefix);
      applyRetention();
    } catch (Exception e) {
      log.error("Backup run {} failed", run.getId(), e);
      run.setStatus("FAILED");
      run.setErrorMessage(e.getMessage());
    } finally {
      run.setFinishedAt(Instant.now());
      backupRunRepository.save(run);
    }
  }

  private Path dumpDatabase(String db) throws IOException, InterruptedException {
    Path dump = Files.createTempFile("backup-" + db, ".sql.gz");
    ProcessBuilder pgDump =
        new ProcessBuilder("pg_dump", "-h", dbHost, "-U", dbUsername, db)
            .redirectErrorStream(false);
    pgDump.environment().put("PGPASSWORD", dbPassword);
    Process process = pgDump.start();
    try (InputStream rawDump = process.getInputStream();
        var gzipOut = new java.util.zip.GZIPOutputStream(Files.newOutputStream(dump))) {
      rawDump.transferTo(gzipOut);
    }
    boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
    if (!finished || process.exitValue() != 0) {
      String stderr = new String(process.getErrorStream().readAllBytes());
      throw new IOException("pg_dump failed for database " + db + ": " + stderr);
    }
    return dump;
  }

  private Path encryptSecrets() {
    try {
      Path tar = Files.createTempFile("secrets", ".tar");
      new ProcessBuilder("tar", "-cf", tar.toString(), "-C", secretsDir, ".").start().waitFor();
      Path encrypted = Files.createTempFile("secrets", ".tar.gpg");
      Files.deleteIfExists(encrypted);
      ProcessBuilder gpg =
          new ProcessBuilder(
              "gpg",
              "--batch",
              "--yes",
              "--passphrase",
              encryptionKey,
              "--symmetric",
              "--cipher-algo",
              "AES256",
              "-o",
              encrypted.toString(),
              tar.toString());
      Process process = gpg.start();
      process.waitFor(1, java.util.concurrent.TimeUnit.MINUTES);
      Files.deleteIfExists(tar);
      return Files.exists(encrypted) ? encrypted : null;
    } catch (Exception e) {
      log.warn("Failed to encrypt secrets for backup, skipping this artifact: {}", e.getMessage());
      return null;
    }
  }

  private long uploadToS3(Path file, String key) throws IOException {
    long size = Files.size(file);
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromFile(file));
    return size;
  }

  private void ensureBucketExists() {
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
    } catch (NoSuchBucketException e) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    }
  }

  private void applyRetention() {
    Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
    var listing = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build());
    for (S3Object object : listing.contents()) {
      if (object.lastModified().isBefore(cutoff)) {
        s3Client.deleteObject(
            software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(object.key())
                .build());
      }
    }
  }
}
