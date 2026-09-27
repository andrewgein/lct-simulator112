package com.simulator112.adminservice.adapter.in.web;

import com.simulator112.adminservice.adapter.in.web.dto.BackupRunResponse;
import com.simulator112.adminservice.application.port.in.ManageBackupsUseCase;
import com.simulator112.adminservice.application.port.out.BackupObjectStorage;
import com.simulator112.adminservice.domain.model.BackupRun;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.data.domain.Page;
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

@RestController
public class BackupController {

  private final ManageBackupsUseCase manageBackups;
  private final BackupObjectStorage backupObjectStorage;

  public BackupController(ManageBackupsUseCase manageBackups, BackupObjectStorage backupObjectStorage) {
    this.manageBackups = manageBackups;
    this.backupObjectStorage = backupObjectStorage;
  }

  @PostMapping("/api/v1/admin/backups/run")
  public BackupRunResponse run(@RequestHeader(value = "X-User-Id", required = false) UUID triggeredBy) {
    BackupRun run = manageBackups.start(triggeredBy);
    manageBackups.runAsync(run.getId(), triggeredBy);
    return BackupRunResponse.from(run);
  }

  @GetMapping("/api/v1/admin/backups")
  public Page<BackupRunResponse> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return manageBackups.list(page, size).map(BackupRunResponse::from);
  }

  @GetMapping("/api/v1/admin/backups/{id}/download")
  public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id) {
    ManageBackupsUseCase.BackupDownload download = manageBackups.prepareDownload(id);
    String prefix = download.prefix();
    List<String> keys = download.keys();

    StreamingResponseBody body =
        outputStream -> {
          try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            for (String key : keys) {
              zip.putNextEntry(new ZipEntry(key.substring(prefix.length())));
              try (InputStream s3Object = backupObjectStorage.open(key)) {
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
