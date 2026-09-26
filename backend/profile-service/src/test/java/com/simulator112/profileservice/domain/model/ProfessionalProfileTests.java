package com.simulator112.profileservice.domain.model;

import com.simulator112.profileservice.domain.exception.InvalidProfessionalProfileException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfessionalProfileTests {

    @Test
    void system112MustNotHaveDdsService() {
        assertDoesNotThrow(() -> new ProfessionalProfile(TrainingTrack.SYSTEM_112, null));
        assertThrows(
                InvalidProfessionalProfileException.class,
                () -> new ProfessionalProfile(TrainingTrack.SYSTEM_112, "AMBULANCE"));
    }

    @Test
    void ddsMustHaveService() {
        assertDoesNotThrow(
                () -> new ProfessionalProfile(TrainingTrack.DDS, "AMBULANCE"));
        assertThrows(
                InvalidProfessionalProfileException.class,
                () -> new ProfessionalProfile(TrainingTrack.DDS, null));
    }
}
