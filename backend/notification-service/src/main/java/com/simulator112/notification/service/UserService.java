package com.simulator112.notification.service;

import com.simulator112.notification.model.User;
import com.simulator112.notification.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User userCreate(UUID userId, String email) {
        User user = userRepository.findById(userId)
                .orElseGet(() -> new User(userId, email));
        user.setEmail(email);
        return userRepository.save(user);
    }

    public String getEmailById(UUID id) {
        return userRepository.findEmailById(id)
                .orElseThrow(() -> new RuntimeException("Почта не найдена"));
    }
}
