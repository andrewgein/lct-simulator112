package com.simulator112.adminservice.adapter.out.persistence;

import com.simulator112.adminservice.adapter.out.persistence.entity.AuditLogJpaEntity;
import com.simulator112.adminservice.adapter.out.persistence.repository.SpringDataAuditLogRepository;
import com.simulator112.adminservice.application.port.in.SearchAuditLogsUseCase;
import com.simulator112.adminservice.domain.model.AuditLogEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogSearchQuery implements SearchAuditLogsUseCase {

  private final SpringDataAuditLogRepository repository;

  @Override
  public Page<AuditLogEntry> search(
      UUID userId, String action, Instant occurredFrom, Instant occurredTo, int page, int size) {
    List<Specification<AuditLogJpaEntity>> predicates = new ArrayList<>();
    if (userId != null) predicates.add((root, query, cb) -> cb.equal(root.get("actorUserId"), userId));
    if (action != null) predicates.add((root, query, cb) -> cb.equal(root.get("action"), action));
    if (occurredFrom != null)
      predicates.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), occurredFrom));
    if (occurredTo != null)
      predicates.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), occurredTo));

    Specification<AuditLogJpaEntity> spec = Specification.allOf(predicates);
    return repository
        .findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")))
        .map(AuditLogPersistenceAdapter::toDomain);
  }
}
