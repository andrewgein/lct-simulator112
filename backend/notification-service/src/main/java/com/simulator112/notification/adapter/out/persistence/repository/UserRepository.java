package com.simulator112.notification.adapter.out.persistence.repository;

import com.simulator112.notification.adapter.out.persistence.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("SELECT u.email FROM User u WHERE u.id = :id")
    Optional<String> findEmailById(@Param("id") UUID userId);

    // Аккаунт мог быть пересоздан с тем же email и новым id
    @Modifying
    @Query("DELETE FROM User u WHERE u.email = :email AND u.id <> :id")
    void deleteStaleByEmail(@Param("email") String email, @Param("id") UUID id);
}
