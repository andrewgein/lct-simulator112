package com.simulator112.adminservice.adapter.in.web;

import com.simulator112.adminservice.adapter.in.web.dto.AuditLogResponse;
import com.simulator112.adminservice.application.port.in.SearchAuditLogsUseCase;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController {

  private final SearchAuditLogsUseCase searchAuditLogs;

  public AuditLogController(SearchAuditLogsUseCase searchAuditLogs) {
    this.searchAuditLogs = searchAuditLogs;
  }

  @GetMapping("/api/v1/admin/audit-logs")
  public Page<AuditLogResponse> search(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {

    Instant from = dateFrom != null ? dateFrom.atZone(ZoneId.systemDefault()).toInstant() : null;
    Instant to = dateTo != null ? dateTo.atZone(ZoneId.systemDefault()).toInstant() : null;

    return searchAuditLogs.search(userId, action, from, to, page, size).map(AuditLogResponse::from);
  }
}
