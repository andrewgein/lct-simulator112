package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.AuditLogResponse;
import com.simulator112.adminservice.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController {

  private final AuditLogRepository auditLogRepository;

  public AuditLogController(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @GetMapping("/api/v1/admin/audit-logs")
  public Page<AuditLogResponse> search(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    // The admin UI sends <input type="datetime-local"> values (no timezone) - interpreted in the
    // server's local zone, which is acceptable precision for an audit-log filter.
    var from = dateFrom != null ? dateFrom.atZone(ZoneId.systemDefault()).toInstant() : null;
    var to = dateTo != null ? dateTo.atZone(ZoneId.systemDefault()).toInstant() : null;
    return auditLogRepository
        .search(userId, action, from, to, PageRequest.of(page, size))
        .map(AuditLogResponse::from);
  }
}
