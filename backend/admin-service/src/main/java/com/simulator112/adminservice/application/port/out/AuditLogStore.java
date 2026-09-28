package com.simulator112.adminservice.application.port.out;

import com.simulator112.adminservice.domain.model.AuditLogEntry;

public interface AuditLogStore {
  AuditLogEntry save(AuditLogEntry entry);
}
