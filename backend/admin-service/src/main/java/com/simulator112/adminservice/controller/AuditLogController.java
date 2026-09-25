package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.AuditLogResponse;
import com.simulator112.adminservice.entity.AuditLogEntry;
import com.simulator112.adminservice.repository.AuditLogRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    Instant from = dateFrom != null ? dateFrom.atZone(ZoneId.systemDefault()).toInstant() : null;
    Instant to = dateTo != null ? dateTo.atZone(ZoneId.systemDefault()).toInstant() : null;

    // Built as a Specification (rather than a hand-rolled "(:param is null or column = :param)"
    // JPQL query) so each parameter is only ever bound where it has a concrete, unambiguous
    // column type. The JPQL null-check pattern hits a known Postgres/Hibernate limitation -
    // "could not determine data type of parameter" - when a bind parameter appears standalone in
    // an IS NULL check with no other type context in that prepared statement.
    List<Specification<AuditLogEntry>> predicates = new ArrayList<>();
    if (userId != null) predicates.add((root, query, cb) -> cb.equal(root.get("actorUserId"), userId));
    if (action != null) predicates.add((root, query, cb) -> cb.equal(root.get("action"), action));
    if (from != null) predicates.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
    if (to != null) predicates.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));

    Specification<AuditLogEntry> spec = Specification.allOf(predicates);
    return auditLogRepository
        .findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")))
        .map(AuditLogResponse::from);
  }
}
