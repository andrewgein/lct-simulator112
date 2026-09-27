package com.simulator112.notification.adapter.out.persistence;

import com.simulator112.notification.application.port.out.UserDirectory;
import com.simulator112.notification.adapter.out.persistence.entity.User;
import com.simulator112.notification.adapter.out.persistence.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserDirectory {
    private final UserRepository userRepository;

    public void upsert(UUID userId, String email) {
        User user = userRepository.findById(userId)
                .orElseGet(() -> new User(userId, email));
        user.setEmail(email);
        userRepository.save(user);
    }

    public String requireEmail(UUID id) {
        return userRepository.findEmailById(id)
                .orElseThrow(() -> new RuntimeException("Почта не найдена"));
    }
}
