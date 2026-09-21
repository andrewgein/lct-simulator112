package com.simulator112.contextmanager.adapter.grpc.mapper;

import com.simulator112.context.grpc.contract.SolutionContext;
import com.simulator112.contextmanager.domain.system112.PersonInfo;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import java.util.UUID;

public final class SolutionContextMapper {
    private SolutionContextMapper() {}

    public static SolutionContext toProto(SolutionCardRevision revision) {
        var builder = SolutionContext.newBuilder()
                .setApplicant(toProto(revision.getApplicant()))
                .setVictim(toProto(revision.getVictim()))
                .setIncidentType(orEmpty(revision.getIncidentType()))
                .putAllAdditionalInfo(revision.getAdditionalInfo())
                .setAdditionalInfoProvided(revision.isAdditionalInfoProvided())
                .setCallId(orEmpty(revision.getCallId()))
                .setRevisionId(orEmpty(revision.getId()))
                .setCardId(orEmpty(revision.getCardId()))
                .setPreviousRevisionId(orEmpty(revision.getPreviousRevisionId()))
                .setVersion(revision.getVersion())
                .setCreatedAt(revision.getCreatedAt() == null ? "" : revision.getCreatedAt().toString());
        if (revision.getMainCardId() != null) builder.setMainCardId(revision.getMainCardId().toString());
        return builder.build();
    }

    private static com.simulator112.context.grpc.contract.PersonInfo toProto(PersonInfo person) {
        if (person == null) return com.simulator112.context.grpc.contract.PersonInfo.getDefaultInstance();
        return com.simulator112.context.grpc.contract.PersonInfo.newBuilder()
                .setPhone(orEmpty(person.phone())).setContactPhone(orEmpty(person.contactPhone()))
                .setLastName(orEmpty(person.lastName())).setFirstName(orEmpty(person.firstName()))
                .setMiddleName(orEmpty(person.middleName())).setAddress(orEmpty(person.address()))
                .setAdditionalInfo(orEmpty(person.additionalInfo())).build();
    }

    private static String orEmpty(String value) { return value == null ? "" : value; }
    private static String orEmpty(UUID value) { return value == null ? "" : value.toString(); }
}
