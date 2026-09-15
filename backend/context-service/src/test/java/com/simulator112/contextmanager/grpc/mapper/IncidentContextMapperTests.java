package com.simulator112.contextmanager.grpc.mapper;

import com.simulator112.contextmanager.model.entity.IncidentContextEntity;
import com.simulator112.incident.grpc.contract.Address;
import com.simulator112.incident.grpc.contract.Applicant;
import com.simulator112.incident.grpc.contract.DialupContext;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.StageContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncidentContextMapperTests {

    @Test
    void preservesAllApplicantAndVictimDetails() {
        Applicant applicant = Applicant.newBuilder()
                .setFirstName("Артём")
                .setPhone("111")
                .setContactPhone("222")
                .setAddress("Адрес заявителя")
                .setAdditionalInfo("Сведения о заявителе")
                .build();
        Applicant victim = Applicant.newBuilder()
                .setFirstName("Сергей")
                .setPhone("333")
                .setContactPhone("444")
                .setAddress("Адрес пострадавшего")
                .setAdditionalInfo("Сведения о пострадавшем")
                .build();

        DialupContext result = roundTrip(applicant, victim);

        assertEquals(applicant, result.getApplicant());
        assertEquals(victim, result.getVictim());
    }

    @Test
    void doesNotInferParticipantAddressesFromIncidentAddress() {
        DialupContext result = roundTrip(
                Applicant.newBuilder().setFirstName("Артём").build(),
                Applicant.newBuilder().setFirstName("Сергей").build());

        assertEquals("", result.getApplicant().getAddress());
        assertEquals("", result.getVictim().getAddress());
    }

    private DialupContext roundTrip(Applicant applicant, Applicant victim) {
        String dialupId = UUID.randomUUID().toString();
        IncidentContext source = IncidentContext.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setAddress(Address.newBuilder()
                        .setCity("Москва")
                        .setStreet("Варшавское шоссе")
                        .setHouse("150")
                        .build())
                .addStages(StageContext.newBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setVictim(victim)
                        .addDialups(DialupContext.newBuilder()
                                .setId(dialupId)
                                .setApplicant(applicant)
                                .setVictim(victim)
                                .build())
                        .build())
                .build();

        IncidentContextEntity stored = IncidentContextMapper.toEntity(source);
        return IncidentContextMapper.toProto(stored.getStages().getFirst().getDialups().getFirst());
    }
}
