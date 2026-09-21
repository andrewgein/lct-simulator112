package com.simulator112.profileservice.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataUserProfileRepository extends JpaRepository<UserProfileJpaEntity, UUID> {
}
