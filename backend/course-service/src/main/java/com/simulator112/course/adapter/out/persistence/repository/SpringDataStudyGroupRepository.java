package com.simulator112.course.adapter.out.persistence.repository;

import com.simulator112.course.adapter.out.persistence.entity.StudyGroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataStudyGroupRepository extends JpaRepository<StudyGroupJpaEntity, UUID> {
}
