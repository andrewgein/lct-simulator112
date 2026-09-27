package com.simulator112.adminservice.adapter.out.persistence;

import com.simulator112.adminservice.adapter.out.persistence.entity.AuditLogJpaEntity;
import com.simulator112.adminservice.adapter.out.persistence.repository.SpringDataAuditLogRepository;
import com.simulator112.adminservice.application.port.out.AuditLogStore;
import com.simulator112.adminservice.domain.model.AuditLogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogStore {

  private final SpringDataAuditLogRepository repository;

  @Override
  public AuditLogEntry save(AuditLogEntry entry) {
    AuditLogJpaEntity saved = repository.save(toEntity(entry));
    return toDomain(saved);
  }

  static AuditLogJpaEntity toEntity(AuditLogEntry source) {
    AuditLogJpaEntity target = new AuditLogJpaEntity();
    target.setId(source.getId());
    target.setOccurredAt(source.getOccurredAt());
    target.setActorUserId(source.getActorUserId());
    target.setActorEmail(source.getActorEmail());
    target.setActorRole(source.getActorRole());
    target.setAction(source.getAction());
    target.setResourceType(source.getResourceType());
    target.setResourceId(source.getResourceId());
    target.setIpAddress(source.getIpAddress());
    target.setDetails(source.getDetails());
    target.setSourceService(source.getSourceService());
    return target;
  }

  static AuditLogEntry toDomain(AuditLogJpaEntity source) {
    AuditLogEntry target = new AuditLogEntry();
    target.setId(source.getId());
    target.setOccurredAt(source.getOccurredAt());
    target.setActorUserId(source.getActorUserId());
    target.setActorEmail(source.getActorEmail());
    target.setActorRole(source.getActorRole());
    target.setAction(source.getAction());
    target.setResourceType(source.getResourceType());
    target.setResourceId(source.getResourceId());
    target.setIpAddress(source.getIpAddress());
    target.setDetails(source.getDetails());
    target.setSourceService(source.getSourceService());
    return target;
  }
}
