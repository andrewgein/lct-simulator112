package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.adapter.in.web.dto.CreateUserProfileRequest;
import com.simulator112.profileservice.application.port.in.UserProfileUseCase;
import com.simulator112.profileservice.domain.exception.InvalidProfessionalProfileException;
import com.simulator112.profileservice.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTests {

    @Mock
    private UserProfileUseCase userProfiles;

    private UserProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new UserProfileController(userProfiles);
    }

    @Test
    void adminCreatesProfileWithoutProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        CreateUserProfileRequest request =
                new CreateUserProfileRequest("Иван", "Иванов", null, null);
        UserProfile profile = UserProfile.create(userId, "Иван", "Иванов");
        when(userProfiles.createProfile(userId, "Иван", "Иванов", null))
                .thenReturn(profile);

        var response = controller.createProfile(userId, "ADMIN", request);

        assertNull(response.trainingTrack());
        assertNull(response.ddsService());
        verify(userProfiles).createProfile(userId, "Иван", "Иванов", null);
    }

    @Test
    void studentCannotCreateProfileWithoutProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        CreateUserProfileRequest request =
                new CreateUserProfileRequest("Иван", "Иванов", null, null);

        assertThrows(
                InvalidProfessionalProfileException.class,
                () -> controller.createProfile(userId, "STUDENT", request));
        verifyNoInteractions(userProfiles);
    }
}
