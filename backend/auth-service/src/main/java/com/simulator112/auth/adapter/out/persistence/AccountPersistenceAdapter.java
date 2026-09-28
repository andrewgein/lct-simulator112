package com.simulator112.auth.adapter.out.persistence;

import com.simulator112.auth.application.port.out.AccountStore;
import com.simulator112.auth.domain.model.Account;
import com.simulator112.auth.domain.model.Role;
import com.simulator112.auth.adapter.out.persistence.entity.User;
import com.simulator112.auth.adapter.out.persistence.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountStore {
    private final UserRepository repository;

    public List<Account> lockAdministrators() {
        return repository.findByRoleForUpdate(Role.ADMIN).stream().map(AccountPersistenceAdapter::toDomain).toList();
    }

    public void deleteById(UUID userId) {
        repository.deleteById(userId);
    }

    public Optional<Account> findByEmail(String email) {
        return repository.findByEmail(email).map(AccountPersistenceAdapter::toDomain);
    }

    public Optional<Account> findById(UUID userId) {
        return repository.findById(userId).map(AccountPersistenceAdapter::toDomain);
    }

    public boolean anyVerified() {
        return repository.existsByEmailVerifiedTrue();
    }

    public List<Account> findAll() {
        return repository.findAll().stream().map(AccountPersistenceAdapter::toDomain).toList();
    }

    public Account save(Account account) {
        User entity = account.id() == null ? new User() : repository.findById(account.id()).orElseGet(User::new);
        entity.setId(account.id());
        entity.setEmail(account.email());
        entity.setPassword(account.passwordHash());
        entity.setRole(account.role());
        entity.setEmailVerified(account.emailVerified());
        if (account.createdAt() != null) entity.setCreatedAt(account.createdAt());
        return toDomain(repository.save(entity));
    }

    private static Account toDomain(User entity) {
        return new Account(entity.getId(), entity.getEmail(), entity.getPassword(), entity.getRole(),
                entity.isEmailVerified(), entity.getCreatedAt());
    }
}
