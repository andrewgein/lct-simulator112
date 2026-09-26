package com.simulator112.profileservice.application.service;

import com.simulator112.profileservice.application.port.out.DispatchServiceCatalogPort;
import com.simulator112.profileservice.application.port.out.UserProfileRepository;
import com.simulator112.profileservice.domain.exception.InvalidProfessionalProfileException;
import com.simulator112.profileservice.domain.exception.ProfessionalProfileNotAssignedException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceTests {

    @Mock
    private UserProfileRepository repository;

    @Mock
    private DispatchServiceCatalogPort dispatchServices;

    private UserProfileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileApplicationService(repository, dispatchServices);
    }

    @Test
    void createsProfileWithProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        ProfessionalProfile professionalProfile =
                new ProfessionalProfile(TrainingTrack.SYSTEM_112, null);
        UserProfile profile = UserProfile.create(
                userId, "Иван", "Иванов", professionalProfile);
        when(repository.save(profile)).thenReturn(profile);

        UserProfile result = service.createProfile(
                userId, "Иван", "Иванов", null, professionalProfile);

        assertEquals(professionalProfile, result.professionalProfile());
        verify(repository).save(profile);
    }

    @Test
    void assignsDdsProfessionalProfile() {
        UUID userId = UUID.randomUUID();
        UserProfile user = UserProfile.create(userId, "Иван", "Иванов");
        ProfessionalProfile professionalProfile =
                new ProfessionalProfile(TrainingTrack.DDS, "MCHS");
        UserProfile updated = user.assignProfessionalProfile(professionalProfile);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(updated)).thenReturn(updated);

        UserProfile result = service.assignProfessionalProfile(userId, professionalProfile);

        assertEquals(professionalProfile, result.professionalProfile());
        verify(dispatchServices).requireService("MCHS");
        verify(repository).save(updated);
    }

    @Test
    void rejectsUnknownDdsService() {
        UUID userId = UUID.randomUUID();
        ProfessionalProfile profile = new ProfessionalProfile(TrainingTrack.DDS, "UNKNOWN");
        doThrow(new InvalidProfessionalProfileException("Служба не найдена в классификаторе: UNKNOWN"))
                .when(dispatchServices).requireService("UNKNOWN");

        assertThrows(InvalidProfessionalProfileException.class,
                () -> service.createProfile(userId, "Иван", "Иванов", null, profile));
    }

    @Test
    void updatesAndClearsPatronymic() {
        UUID userId = UUID.randomUUID();
        UserProfile user = UserProfile.create(userId, "Иван", "Иванов", "Иванович", null);
        UserProfile updated = user.updatePersonalData("Иван", "Иванов", "");
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(updated)).thenReturn(updated);

        UserProfile result = service.updateProfile(userId, "Иван", "Иванов", "", null);

        assertEquals("", result.patronymic());
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
