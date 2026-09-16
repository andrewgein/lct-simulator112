package com.simulator112.userservice.repository;

import com.simulator112.userservice.entity.UserProfile;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {}
