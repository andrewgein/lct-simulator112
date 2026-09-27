package com.simulator112.adminservice.application.service;

import com.simulator112.adminservice.application.port.in.RecordAuditEventUseCase;
import com.simulator112.adminservice.application.port.out.AuditLogStore;
import com.simulator112.adminservice.domain.model.AuditLogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogApplicationService implements RecordAuditEventUseCase {

  private final AuditLogStore auditLogStore;

  @Override
  public void record(AuditLogEntry entry) {
    auditLogStore.save(entry);
  }
}
