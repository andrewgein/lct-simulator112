package com.simulator112.incident.repository.classifier;

import com.simulator112.incident.model.entity.classifier.ClassifierCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassifierCategoryRepository extends JpaRepository<ClassifierCategoryEntity, UUID> {

    List<ClassifierCategoryEntity> findAllByOrderByPositionAsc();
}
