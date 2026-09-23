package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.adapter.in.web.dto.CreateUserProfileRequest;
import com.simulator112.profileservice.adapter.in.web.dto.UpdateUserProfileRequest;
import com.simulator112.profileservice.application.port.in.UserProfileUseCase;
import com.simulator112.profileservice.domain.exception.InvalidProfessionalProfileException;
import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.TrainingTrack;
import com.simulator112.profileservice.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                new CreateUserProfileRequest("Иван", "Иванов", "Иванович", null, null);
        UserProfile profile = UserProfile.create(userId, "Иван", "Иванов", "Иванович", null);
        when(userProfiles.createProfile(userId, "Иван", "Иванов", "Иванович", null))
                .thenReturn(profile);

        var response = controller.createProfile(userId, "ADMIN", request);

        assertEquals("Иванович", response.patronymic());
        assertNull(response.trainingTrack());
        assertNull(response.ddsService());
        verify(userProfiles).createProfile(userId, "Иван", "Иванов", "Иванович", null);
    }

    @Test
    void supervisorCreatesProfileWithoutProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        CreateUserProfileRequest request =
                new CreateUserProfileRequest("Иван", "Иванов", null, null, null);
        UserProfile profile = UserProfile.create(userId, "Иван", "Иванов");
        when(userProfiles.createProfile(userId, "Иван", "Иванов", null, null))
                .thenReturn(profile);

        var response = controller.createProfile(userId, "SUPERVISOR", request);

        assertNull(response.trainingTrack());
        assertNull(response.ddsService());
        verify(userProfiles).createProfile(userId, "Иван", "Иванов", null, null);
    }

    @Test
    void supervisorUpdatesOptionalProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        ProfessionalProfile professionalProfile =
                new ProfessionalProfile(TrainingTrack.DDS, DdsService.FIRE);
        UpdateUserProfileRequest request =
                new UpdateUserProfileRequest("Иван", "Иванов", null, TrainingTrack.DDS, DdsService.FIRE);
        UserProfile profile = UserProfile.create(userId, "Иван", "Иванов", null, professionalProfile);
        when(userProfiles.updateProfile(userId, "Иван", "Иванов", null, professionalProfile))
                .thenReturn(profile);

        var response = controller.updateProfile(userId, "SUPERVISOR", request);

        assertEquals(TrainingTrack.DDS, response.trainingTrack());
        assertEquals(DdsService.FIRE, response.ddsService());
        verify(userProfiles).updateProfile(userId, "Иван", "Иванов", null, professionalProfile);
    }

    @Test
    void studentCannotCreateProfileWithoutProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        CreateUserProfileRequest request =
                new CreateUserProfileRequest("Иван", "Иванов", null, null, null);

        assertThrows(
                InvalidProfessionalProfileException.class,
                () -> controller.createProfile(userId, "STUDENT", request));
        verifyNoInteractions(userProfiles);
    }
}
