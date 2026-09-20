package com.simulator112.classifier.adapter.out.persistence.repository;

import com.simulator112.classifier.adapter.out.persistence.entity.ClassifierCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassifierCategoryRepository extends JpaRepository<ClassifierCategoryEntity, UUID> {

    List<ClassifierCategoryEntity> findAllByOrderByPositionAsc();
}
