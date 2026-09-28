package com.simulator112.adminservice.application.port.in;

import com.simulator112.adminservice.domain.model.AuditLogEntry;

public interface RecordAuditEventUseCase {
  void record(AuditLogEntry entry);
}
