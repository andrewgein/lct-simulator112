package com.simulator112.contextmanager.grpc.mapper;

import com.simulator112.context.grpc.contract.SolutionContext;
import com.simulator112.contextmanager.dto.request.PersonInfoRequest;
import com.simulator112.contextmanager.dto.request.SolutionContextRequest;
import com.simulator112.contextmanager.dto.response.SolutionContextView;
import com.simulator112.contextmanager.model.embeddable.PersonInfo;
import com.simulator112.contextmanager.model.entity.SolutionContextEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SolutionContextMapper {
    private SolutionContextMapper() {}

    public static void fillSnapshot(SolutionContextEntity entity, SolutionContextRequest request) {
        entity.setApplicant(toEntity(request.applicant()));
        entity.setVictim(toEntity(request.victim()));
        entity.setAdditionalInfoProvided(request.additionalInfo() != null);
        if (request.additionalInfo() != null) {
            entity.getAdditionalInfo().putAll(request.additionalInfo());
        }
        entity.setIncidentType(request.incidentType());
    }

    private static PersonInfo toEntity(PersonInfoRequest request) {
        if (request == null) {
            return new PersonInfo();
        }
        return new PersonInfo(
                request.phone(),
                request.contactPhone(),
                request.lastName(),
                request.firstName(),
                request.middleName(),
                request.address(),
                request.additionalInfo());
    }

    public static SolutionContextView toView(SolutionContextEntity entity) {
        return new SolutionContextView(
                entity.getId(),
                entity.getCardId(),
                entity.getPreviousRevisionId(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getDialupId(),
                entity.getParentCardId(),
                entity.getDuplicateOfCardId(),
                toRequest(entity.getApplicant()),
                toRequest(entity.getVictim()),
                Map.copyOf(entity.getAdditionalInfo()),
                entity.getIncidentType(),
                entity.getCreatedAt()
            );
    }

    public static SolutionContextView toAssembledView(List<SolutionContextEntity> revisions) {
        if (revisions.isEmpty()) {
            throw new IllegalArgumentException("Для сборки карточки нужна хотя бы одна ревизия");
        }

        List<SolutionContextEntity> ordered = revisions.stream()
                .sorted(Comparator.comparingLong(SolutionContextEntity::getVersion))
                .toList();
        PersonInfoRequest applicant = null;
        PersonInfoRequest victim = null;
        Map<String, String> additionalInfo = Map.of();
        String incidentType = null;

        for (SolutionContextEntity revision : ordered) {
            applicant = merge(applicant, revision.getApplicant());
            victim = merge(victim, revision.getVictim());
            if (revision.isAdditionalInfoProvided()) {
                additionalInfo = Map.copyOf(revision.getAdditionalInfo());
            }
            if (revision.getIncidentType() != null) {
                incidentType = revision.getIncidentType();
            }
        }

        SolutionContextEntity latest = ordered.get(ordered.size() - 1);
        return new SolutionContextView(
                latest.getId(), latest.getCardId(), latest.getPreviousRevisionId(), latest.getVersion(),
                latest.getStatus(), latest.getDialupId(), latest.getParentCardId(),
                latest.getDuplicateOfCardId(), applicant, victim, additionalInfo, incidentType,
                latest.getCreatedAt());
    }

    private static PersonInfoRequest merge(PersonInfoRequest previous, PersonInfo next) {
        if (next == null) {
            return previous;
        }
        return new PersonInfoRequest(
                inheritText(next.getPhone(), previous == null ? null : previous.phone()),
                inheritText(next.getContactPhone(), previous == null ? null : previous.contactPhone()),
                inheritText(next.getLastName(), previous == null ? null : previous.lastName()),
                inheritText(next.getFirstName(), previous == null ? null : previous.firstName()),
                inheritText(next.getMiddleName(), previous == null ? null : previous.middleName()),
                inheritText(next.getAddress(), previous == null ? null : previous.address()),
                inheritText(next.getAdditionalInfo(), previous == null ? null : previous.additionalInfo()));
    }

    private static String inheritText(String current, String previous) {
        return current == null ? previous : current;
    }

    private static PersonInfoRequest toRequest(PersonInfo entity) {
        if (entity == null) {
            return null;
        }
        return new PersonInfoRequest(
                entity.getPhone(),
                entity.getContactPhone(),
                entity.getLastName(),
                entity.getFirstName(),
                entity.getMiddleName(),
                entity.getAddress(),
                entity.getAdditionalInfo());
    }

    public static SolutionContext toProto(SolutionContextEntity entity) {
        SolutionContext.Builder builder = SolutionContext.newBuilder()
                .setApplicant(toProto(entity.getApplicant()))
                .setVictim(toProto(entity.getVictim()))
                .setIncidentType(orEmpty(entity.getIncidentType()))
                .putAllAdditionalInfo(entity.getAdditionalInfo())
                .setAdditionalInfoProvided(entity.isAdditionalInfoProvided())
                .setDialupId(orEmpty(entity.getDialupId()))
                .setRevisionId(orEmpty(entity.getId()))
                .setCardId(orEmpty(entity.getCardId()))
                .setPreviousRevisionId(orEmpty(entity.getPreviousRevisionId()))
                .setVersion(entity.getVersion())
                .setStatus(com.simulator112.context.grpc.contract.SolutionContextStatus.valueOf(
                        entity.getStatus().name()))
                .setCreatedAt(entity.getCreatedAt() == null ? "" : entity.getCreatedAt().toString());

        if (entity.getParentCardId() != null) {
            builder.setParentId(entity.getParentCardId().toString());
        }
        if (entity.getDuplicateOfCardId() != null) {
            builder.setDuplicateOfId(entity.getDuplicateOfCardId().toString());
        }
        return builder.build();
    }

    private static com.simulator112.context.grpc.contract.PersonInfo toProto(PersonInfo entity) {
        if (entity == null) {
            return com.simulator112.context.grpc.contract.PersonInfo.getDefaultInstance();
        }
        return com.simulator112.context.grpc.contract.PersonInfo.newBuilder()
                .setPhone(orEmpty(entity.getPhone()))
                .setContactPhone(orEmpty(entity.getContactPhone()))
                .setLastName(orEmpty(entity.getLastName()))
                .setFirstName(orEmpty(entity.getFirstName()))
                .setMiddleName(orEmpty(entity.getMiddleName()))
                .setAddress(orEmpty(entity.getAddress()))
                .setAdditionalInfo(orEmpty(entity.getAdditionalInfo()))
                .build();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String orEmpty(UUID value) {
        return value == null ? "" : value.toString();
    }
}
