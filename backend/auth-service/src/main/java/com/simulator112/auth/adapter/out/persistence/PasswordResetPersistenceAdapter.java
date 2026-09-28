package com.simulator112.auth.adapter.out.persistence;

import com.simulator112.auth.application.port.out.PasswordResetStore;
import com.simulator112.auth.domain.model.OneTimeToken;
import com.simulator112.auth.adapter.out.persistence.entity.PasswordResetToken;
import com.simulator112.auth.adapter.out.persistence.repository.PasswordResetTokenRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PasswordResetPersistenceAdapter implements PasswordResetStore {
    private final PasswordResetTokenRepository repository;

    public Optional<OneTimeToken> findByToken(String token) {
        return repository.findByToken(token).map(entity -> new OneTimeToken(
                entity.getToken(), entity.getUserId(), entity.getExpiresAt(), entity.isUsed()));
    }

    public void deleteAllByUserId(UUID userId) {
        repository.deleteAllByUserId(userId);
    }

    public void save(OneTimeToken value) {
        PasswordResetToken entity = repository.findByToken(value.token()).orElseGet(PasswordResetToken::new);
        entity.setToken(value.token());
        entity.setUserId(value.userId());
        entity.setExpiresAt(value.expiresAt());
        entity.setUsed(value.used());
        repository.save(entity);
    }
}
