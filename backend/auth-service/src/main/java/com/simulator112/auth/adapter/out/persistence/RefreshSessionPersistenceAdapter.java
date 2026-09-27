package com.simulator112.auth.adapter.out.persistence;

import com.simulator112.auth.application.port.out.RefreshSessionStore;
import com.simulator112.auth.domain.model.RefreshSession;
import com.simulator112.auth.adapter.out.persistence.entity.RefreshToken;
import com.simulator112.auth.adapter.out.persistence.repository.RefreshTokenRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshSessionPersistenceAdapter implements RefreshSessionStore {
    private final RefreshTokenRepository repository;

    public Optional<RefreshSession> findByToken(String token) {
        return repository.findByToken(token).map(entity -> new RefreshSession(
                entity.getToken(), entity.getUserId(), entity.getExpiresAt(), entity.isRevoked()));
    }

    public void deleteAllByUserId(UUID userId) {
        repository.deleteAllByUserId(userId);
    }

    public void save(RefreshSession value) {
        RefreshToken entity = repository.findByToken(value.token()).orElseGet(RefreshToken::new);
        entity.setToken(value.token());
        entity.setUserId(value.userId());
        entity.setExpiresAt(value.expiresAt());
        entity.setRevoked(value.revoked());
        repository.save(entity);
    }
}
