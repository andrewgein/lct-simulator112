package com.simulator112.incident.repository;

import com.simulator112.incident.model.entity.TypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TypeRepository extends JpaRepository<TypeEntity, UUID> {

    List<TypeEntity> findAllByOrderByServiceTypeAscTypeIdAsc();
}
