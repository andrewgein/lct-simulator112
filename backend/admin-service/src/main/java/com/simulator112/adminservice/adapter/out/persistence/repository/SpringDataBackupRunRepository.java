package com.simulator112.adminservice.adapter.out.persistence.repository;

import com.simulator112.adminservice.adapter.out.persistence.entity.BackupRunJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBackupRunRepository extends JpaRepository<BackupRunJpaEntity, UUID> {
  Page<BackupRunJpaEntity> findAllByOrderByStartedAtDesc(Pageable pageable);
}
