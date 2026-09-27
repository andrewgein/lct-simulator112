package com.simulator112.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Account(UUID id, String email, String passwordHash, Role role,
                      boolean emailVerified, LocalDateTime createdAt) {
    public Account verify(Role assignedRole) {
        return new Account(id, email, passwordHash, assignedRole, true, createdAt);
    }

    public Account withRole(Role newRole) {
        return new Account(id, email, passwordHash, newRole, emailVerified, createdAt);
    }

    public Account withPassword(String hash) {
        return new Account(id, email, hash, role, emailVerified, createdAt);
    }
}
