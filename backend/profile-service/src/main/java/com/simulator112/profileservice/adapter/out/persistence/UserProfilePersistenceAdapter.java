package com.simulator112.profileservice.adapter.out.persistence;

import com.simulator112.profileservice.application.port.out.UserProfileRepository;
import com.simulator112.profileservice.domain.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserProfilePersistenceAdapter implements UserProfileRepository {

    private final SpringDataUserProfileRepository repository;

    @Override
    public Optional<UserProfile> findById(UUID userId) {
        return repository.findById(userId).map(UserProfilePersistenceMapper::toDomain);
    }

    @Override
    public List<UserProfile> findAll() {
        return repository.findAll().stream().map(UserProfilePersistenceMapper::toDomain).toList();
    }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileJpaEntity entity = repository.findById(profile.userId()).orElseGet(
                () -> UserProfilePersistenceMapper.newEntity(profile));
        UserProfilePersistenceMapper.updateEntity(entity, profile);
        return UserProfilePersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public void delete(UserProfile profile) {
        repository.deleteById(profile.userId());
    }
}
