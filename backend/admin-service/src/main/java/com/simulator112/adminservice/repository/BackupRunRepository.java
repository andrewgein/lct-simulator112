package com.simulator112.adminservice.repository;

import com.simulator112.adminservice.entity.BackupRun;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupRunRepository extends JpaRepository<BackupRun, UUID> {
  Page<BackupRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
