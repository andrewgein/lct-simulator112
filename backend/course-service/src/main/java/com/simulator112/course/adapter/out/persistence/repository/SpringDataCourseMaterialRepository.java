package com.simulator112.course.adapter.out.persistence.repository;

import com.simulator112.course.adapter.out.persistence.entity.CourseMaterialJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCourseMaterialRepository extends JpaRepository<CourseMaterialJpaEntity, UUID> {
}
