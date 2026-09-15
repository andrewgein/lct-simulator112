package com.simulator112.contextmanager.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository; 
import com.simulator112.contextmanager.model.entity.Context; 



public interface ContextRepository extends JpaRepository<Context, UUID> {
}
