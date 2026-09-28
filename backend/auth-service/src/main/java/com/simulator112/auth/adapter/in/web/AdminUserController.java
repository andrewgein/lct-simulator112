package com.simulator112.auth.adapter.in.web;

import com.simulator112.auth.application.port.in.DeleteUserUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final DeleteUserUseCase users;

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, Authentication authentication) {
        users.deleteUser(userId, (UUID) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
