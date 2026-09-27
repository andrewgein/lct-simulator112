package com.simulator112.adminservice.adapter.out.persistence;

import com.simulator112.adminservice.adapter.out.persistence.entity.BackupRunJpaEntity;
import com.simulator112.adminservice.adapter.out.persistence.repository.SpringDataBackupRunRepository;
import com.simulator112.adminservice.application.port.out.BackupRunStore;
import com.simulator112.adminservice.domain.model.BackupRun;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BackupRunPersistenceAdapter implements BackupRunStore {

  private final SpringDataBackupRunRepository repository;

  @Override
  public BackupRun save(BackupRun run) {
    return toDomain(repository.save(toEntity(run)));
  }

  @Override
  public Optional<BackupRun> findById(UUID id) {
    return repository.findById(id).map(BackupRunPersistenceAdapter::toDomain);
  }

  @Override
  public Page<BackupRun> findAllByOrderByStartedAtDesc(int page, int size) {
    return repository.findAllByOrderByStartedAtDesc(PageRequest.of(page, size)).map(BackupRunPersistenceAdapter::toDomain);
  }

  private static BackupRunJpaEntity toEntity(BackupRun source) {
    BackupRunJpaEntity target = new BackupRunJpaEntity();
    target.setId(source.getId());
    target.setStartedAt(source.getStartedAt());
    target.setFinishedAt(source.getFinishedAt());
    target.setStatus(source.getStatus());
    target.setSizeBytes(source.getSizeBytes());
    target.setObjectKey(source.getObjectKey());
    target.setTriggeredBy(source.getTriggeredBy());
    target.setErrorMessage(source.getErrorMessage());
    return target;
  }

  private static BackupRun toDomain(BackupRunJpaEntity source) {
    BackupRun target = new BackupRun();
    target.setId(source.getId());
    target.setStartedAt(source.getStartedAt());
    target.setFinishedAt(source.getFinishedAt());
    target.setStatus(source.getStatus());
    target.setSizeBytes(source.getSizeBytes());
    target.setObjectKey(source.getObjectKey());
    target.setTriggeredBy(source.getTriggeredBy());
    target.setErrorMessage(source.getErrorMessage());
    return target;
  }
}
