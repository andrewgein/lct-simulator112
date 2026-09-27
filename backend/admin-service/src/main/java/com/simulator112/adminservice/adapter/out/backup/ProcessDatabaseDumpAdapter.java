package com.simulator112.adminservice.adapter.out.backup;

import com.simulator112.adminservice.application.port.out.DatabaseDumpPort;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProcessDatabaseDumpAdapter implements DatabaseDumpPort {

  private final String dbHost;
  private final List<String> databases;
  private final String dbUsername;
  private final String dbPassword;
  private final String secretsDir;
  private final String encryptionKey;

  public ProcessDatabaseDumpAdapter(
      @Value("${backup.db-host}") String dbHost,
      @Value("${backup.db-databases}") String databasesCsv,
      @Value("${backup.db-username}") String dbUsername,
      @Value("${backup.db-password}") String dbPassword,
      @Value("${backup.secrets-dir}") String secretsDir,
      @Value("${backup.encryption-key}") String encryptionKey) {
    this.dbHost = dbHost;
    this.databases = List.of(databasesCsv.split(","));
    this.dbUsername = dbUsername;
    this.dbPassword = dbPassword;
    this.secretsDir = secretsDir;
    this.encryptionKey = encryptionKey;
  }

  @Override
  public List<String> configuredDatabases() {
    return databases;
  }

  @Override
  public Path dumpDatabase(String database) throws IOException, InterruptedException {
    Path dump = Files.createTempFile("backup-" + database, ".sql.gz");
    ProcessBuilder pgDump =
        new ProcessBuilder("pg_dump", "-h", dbHost, "-U", dbUsername, database).redirectErrorStream(false);
    pgDump.environment().put("PGPASSWORD", dbPassword);
    Process process = pgDump.start();
    try (InputStream rawDump = process.getInputStream();
        GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(dump))) {
      rawDump.transferTo(gzipOut);
    }
    boolean finished = process.waitFor(5, TimeUnit.MINUTES);
    if (!finished || process.exitValue() != 0) {
      String stderr = new String(process.getErrorStream().readAllBytes());
      throw new IOException("pg_dump failed for database " + database + ": " + stderr);
    }
    return dump;
  }

  @Override
  public Optional<Path> encryptSecrets() {
    if (secretsDir == null || secretsDir.isBlank() || encryptionKey == null || encryptionKey.isBlank()) {
      return Optional.empty();
    }
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
      process.waitFor(1, TimeUnit.MINUTES);
      Files.deleteIfExists(tar);
      return Files.exists(encrypted) ? Optional.of(encrypted) : Optional.empty();
    } catch (Exception e) {
      log.warn("Failed to encrypt secrets for backup, skipping this artifact: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
