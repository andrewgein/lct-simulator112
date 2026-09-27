package com.simulator112.adminservice.application.port.in;

import com.simulator112.adminservice.domain.model.AuditLogEntry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface SearchAuditLogsUseCase {
  Page<AuditLogEntry> search(
      UUID userId, String action, Instant occurredFrom, Instant occurredTo, int page, int size);
}
