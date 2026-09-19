package com.simulator112.incident.repository.classifier;

import com.simulator112.incident.model.entity.classifier.ClassifierEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClassifierEntryRepository extends JpaRepository<ClassifierEntryEntity, UUID> {

    Optional<ClassifierEntryEntity> findByCode(String code);
}
