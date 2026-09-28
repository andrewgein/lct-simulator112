package com.simulator112.auth.adapter.out.persistence;

import com.simulator112.auth.application.port.out.EmailVerificationStore;
import com.simulator112.auth.domain.model.OneTimeToken;
import com.simulator112.auth.adapter.out.persistence.entity.EmailVerificationToken;
import com.simulator112.auth.adapter.out.persistence.repository.EmailVerificationTokenRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EmailVerificationPersistenceAdapter implements EmailVerificationStore {
    private final EmailVerificationTokenRepository repository;

    public void deleteAllByUserId(UUID userId) {
        repository.deleteAllByUserId(userId);
    }

    public Optional<OneTimeToken> findByToken(String token) {
        return repository.findByToken(token).map(entity -> new OneTimeToken(
                entity.getToken(), entity.getUserId(), entity.getExpiresAt(), entity.isUsed()));
    }

    public void save(OneTimeToken value) {
        EmailVerificationToken entity = repository.findByToken(value.token()).orElseGet(EmailVerificationToken::new);
        entity.setToken(value.token());
        entity.setUserId(value.userId());
        entity.setExpiresAt(value.expiresAt());
        entity.setUsed(value.used());
        repository.save(entity);
    }
}
