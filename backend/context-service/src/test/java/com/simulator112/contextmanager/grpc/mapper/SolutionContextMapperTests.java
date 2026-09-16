package com.simulator112.contextmanager.grpc.mapper;

import com.simulator112.contextmanager.model.embeddable.PersonInfo;
import com.simulator112.contextmanager.model.entity.SolutionContextEntity;
import com.simulator112.contextmanager.model.enums.SolutionContextStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolutionContextMapperTests {

    @Test
    void assemblesCardByInheritingNullFieldsAndClearingBlankFields() {
        UUID cardId = UUID.randomUUID();
        SolutionContextEntity first = revision(cardId, 1);
        first.setApplicant(new PersonInfo("111", null, "Иванов", "Иван", null, "Старый адрес", null));
        first.setIncidentType("FIRE");
        first.setAdditionalInfoProvided(true);
        first.getAdditionalInfo().putAll(Map.of("floor", "2", "door", "left"));

        SolutionContextEntity second = revision(cardId, 2);
        second.setApplicant(new PersonInfo("", "222", "", "", "", "Новый адрес", ""));
        second.setIncidentType("");
        second.setAdditionalInfoProvided(false);

        SolutionContextEntity third = revision(cardId, 3);
        third.setApplicant(new PersonInfo());
        third.setAdditionalInfoProvided(true);
        third.getAdditionalInfo().put("floor", "3");

        var result = SolutionContextMapper.toAssembledView(List.of(third, first, second));

        assertEquals(3, result.version());
        assertEquals("", result.applicant().phone());
        assertEquals("222", result.applicant().contactPhone());
        assertEquals("", result.applicant().lastName());
        assertEquals("Новый адрес", result.applicant().address());
        assertEquals("", result.incidentType());
        assertEquals(Map.of("floor", "3"), result.additionalInfo());
    }

    private SolutionContextEntity revision(UUID cardId, long version) {
        SolutionContextEntity entity = new SolutionContextEntity();
        entity.setId(UUID.randomUUID());
        entity.setCardId(cardId);
        entity.setVersion(version);
        entity.setStatus(SolutionContextStatus.ACTIVE);
        entity.setDialupId(UUID.randomUUID());
        return entity;
    }
}
