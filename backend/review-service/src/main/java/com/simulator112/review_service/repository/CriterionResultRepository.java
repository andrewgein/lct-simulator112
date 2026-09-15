package com.simulator112.review_service.repository;

import com.simulator112.review_service.model.entity.CriterionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CriterionResultRepository extends JpaRepository<CriterionResult, UUID> {
}
