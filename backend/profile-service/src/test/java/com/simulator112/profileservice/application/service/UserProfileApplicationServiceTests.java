package com.simulator112.profileservice.application.service;

import com.simulator112.profileservice.application.port.out.UserProfileRepository;
import com.simulator112.profileservice.domain.exception.ProfessionalProfileNotAssignedException;
import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.TrainingTrack;
import com.simulator112.profileservice.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceTests {

    @Mock
    private UserProfileRepository repository;

    private UserProfileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileApplicationService(repository);
    }

    @Test
    void assignsDdsProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        UserProfile user = UserProfile.create(userId, "Иван", "Иванов");
        ProfessionalProfile professionalProfile =
                new ProfessionalProfile(TrainingTrack.DDS, DdsService.FIRE);
        UserProfile updated = user.assignProfessionalProfile(professionalProfile);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(updated)).thenReturn(updated);

        UserProfile result = service.assignProfessionalProfile(userId, professionalProfile);

        assertEquals(professionalProfile, result.professionalProfile());
        verify(repository).save(updated);
    }

    @Test
    void rejectsReadingUnassignedProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        when(repository.findById(userId))
                .thenReturn(Optional.of(UserProfile.create(userId, "Иван", "Иванов")));

        assertThrows(
                ProfessionalProfileNotAssignedException.class,
                () -> service.getProfessionalProfile(userId));
    }
}
