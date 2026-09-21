package com.simulator112.contextmanager.adapter.grpc.mapper;

import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolutionContextPresenceTests {
    @Test
    void transmitsExplicitPresenceForBothOmittedAndClearedMaps() {
        var entity = new SolutionCardRevision();
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
