package com.simulator112.contextmanager.grpc.mapper;

import com.simulator112.contextmanager.model.entity.SolutionContextEntity;
import com.simulator112.contextmanager.model.enums.SolutionContextStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolutionContextPresenceTests {
    @Test
    void transmitsExplicitPresenceForBothOmittedAndClearedMaps() {
        var entity = new SolutionContextEntity();
        entity.setStatus(SolutionContextStatus.ACTIVE);
        entity.setAdditionalInfoProvided(false);
        var omitted = SolutionContextMapper.toProto(entity);
        assertTrue(omitted.hasAdditionalInfoProvided());
        assertFalse(omitted.getAdditionalInfoProvided());
        entity.setAdditionalInfoProvided(true);
        var cleared = SolutionContextMapper.toProto(entity);
        assertTrue(cleared.hasAdditionalInfoProvided());
        assertTrue(cleared.getAdditionalInfoProvided());
        assertEquals(0, cleared.getAdditionalInfoCount());
    }
}
