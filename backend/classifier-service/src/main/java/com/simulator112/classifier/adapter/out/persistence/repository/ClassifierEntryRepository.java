package com.simulator112.classifier.adapter.out.persistence.repository;

import com.simulator112.classifier.adapter.out.persistence.entity.ClassifierEntryEntity;
import com.simulator112.classifier.domain.model.ClassifierCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface ClassifierEntryRepository extends JpaRepository<ClassifierEntryEntity, UUID> {

    Optional<ClassifierEntryEntity> findByCode(String code);

    @Query("select new com.simulator112.classifier.domain.model.ClassifierCandidate(e.code, e.category.name, e.finalName) from ClassifierEntryEntity e")
    List<ClassifierCandidate> findCandidates();
}
