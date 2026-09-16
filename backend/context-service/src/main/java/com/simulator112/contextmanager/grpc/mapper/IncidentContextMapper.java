package com.simulator112.contextmanager.grpc.mapper;

import com.simulator112.contextmanager.model.embeddable.Address;
import com.simulator112.contextmanager.model.embeddable.Applicant;
import com.simulator112.contextmanager.model.embeddable.DispatcherCriteria;
import com.simulator112.contextmanager.model.embeddable.IncidentTypeInfo;
import com.simulator112.contextmanager.model.entity.DialupContextEntity;
import com.simulator112.contextmanager.model.entity.IncidentContextEntity;
import com.simulator112.contextmanager.model.entity.StageAdditionalInfoContextEntity;
import com.simulator112.contextmanager.model.entity.StageContextEntity;
import com.simulator112.contextmanager.model.enums.ServiceType;
import com.simulator112.shared.dto.EmotionalState;
import com.simulator112.incident.grpc.contract.IncidentContext;

import java.util.UUID;

public final class IncidentContextMapper {

    private IncidentContextMapper() {
    }

    public static IncidentContextEntity toEntity(IncidentContext proto) {
        IncidentContextEntity entity = new IncidentContextEntity();
        entity.setSourceIncidentId(proto.getId());
        entity.setTitle(proto.getTitle());
        entity.setAddress(toEntity(proto.getAddress()));
        entity.setDispatcherCriteria(toEntity(proto.getDispatcherCriteria()));
        proto.getStagesList().forEach(stage -> entity.addStage(toEntity(stage)));
        return entity;
    }

    private static StageContextEntity toEntity(com.simulator112.incident.grpc.contract.StageContext proto) {
        StageContextEntity entity = new StageContextEntity();
        entity.setSourceStageId(UUID.fromString(proto.getId()));
        entity.setTitle(proto.getTitle());
        entity.setPosition(proto.getPosition());
        entity.setType(toEntity(proto.getType()));
        entity.setDescription(proto.getDescription());
        entity.setVictim(toEntity(proto.getVictim()));
        proto.getAdditionalInfoList().forEach(info -> {
            StageAdditionalInfoContextEntity value = new StageAdditionalInfoContextEntity();
            value.setSourceAdditionalInfoId(info.getAdditionalInfoId());
            value.setFieldCode(info.getFieldCode());
            value.setFieldName(info.getFieldName());
            value.setFieldType(info.getFieldType().name());
            value.setRequired(info.getRequired());
            value.setFieldValue(info.getFieldValue());
            entity.addAdditionalInfo(value);
        });
        proto.getDialupsList().forEach(dialup -> entity.addDialup(toEntity(dialup)));
        return entity;
    }

    private static DialupContextEntity toEntity(com.simulator112.incident.grpc.contract.DialupContext proto) {
        DialupContextEntity entity = new DialupContextEntity();
        entity.setSourceDialupId(UUID.fromString(proto.getId()));
        entity.setPosition(proto.getPosition());
        com.simulator112.incident.grpc.contract.DialupDetails details = proto.getDialupDetails();
        entity.setKnownFacts(details.getKnownFactsList());
        entity.setHiddenFacts(details.getHiddenFactsList());
        entity.setAiContext(details.getAiContext());
        entity.setGender(com.simulator112.contextmanager.model.enums.Gender.valueOf(details.getGender().name()));
        entity.setEmotionalState(details.getEmotionalState());
        entity.setApplicant(toEntity(proto.getApplicant()));
        return entity;
    }

    private static IncidentTypeInfo toEntity(com.simulator112.incident.grpc.contract.IncidentTypeInfo proto) {
        return new IncidentTypeInfo(
                proto.getId(),
                proto.getTypeId(),
                ServiceType.valueOf(proto.getServiceType().name()),
                proto.getTypeName());
    }

    private static Address toEntity(com.simulator112.incident.grpc.contract.Address proto) {
        return new Address(
                proto.getCity(),
                proto.getStreet(),
                proto.getHouse(),
                proto.getBuilding(),
                proto.getApartment(),
                proto.getFloor());
    }

    private static Applicant toEntity(com.simulator112.incident.grpc.contract.Applicant proto) {
        return new Applicant(
                proto.getFirstName(),
                proto.getLastName(),
                proto.getMiddleName(),
                proto.getAge(),
                proto.getPhone(),
                proto.getContactPhone(),
                proto.getAddress(),
                proto.getAdditionalInfo(),
                EmotionalState.valueOf(proto.getEmotionalState().name()));
    }

    private static DispatcherCriteria toEntity(com.simulator112.incident.grpc.contract.DispatcherCriteria proto) {
        return new DispatcherCriteria(
                proto.getRequiredQuestionsList(),
                proto.getExpectedActionsList(),
                proto.getCriticalMistakesList());
    }

    public static IncidentContext toProto(IncidentContextEntity entity) {
        IncidentContext.Builder builder = IncidentContext.newBuilder()
                .setId(orEmpty(entity.getSourceIncidentId()))
                .setTitle(orEmpty(entity.getTitle()))
                .setAddress(toProto(entity.getAddress()))
                .setDispatcherCriteria(toProto(entity.getDispatcherCriteria()));

        entity.getStages().forEach(stage -> builder.addStages(toStageProto(stage)));

        return builder.build();
    }

    private static com.simulator112.incident.grpc.contract.StageContext toStageProto(StageContextEntity entity) {
        com.simulator112.incident.grpc.contract.StageContext.Builder builder =
                com.simulator112.incident.grpc.contract.StageContext.newBuilder()
                        .setId(entity.getSourceStageId().toString())
                        .setTitle(orEmpty(entity.getTitle()))
                        .setPosition(entity.getPosition())
                        .setType(toProto(entity.getType()))
                        .setDescription(orEmpty(entity.getDescription()))
                        .setVictim(toProto(entity.getVictim()))
                        .addAllAdditionalInfo(entity.getAdditionalInfo().stream().map(value ->
                                com.simulator112.incident.grpc.contract.IncidentAdditionalInfo.newBuilder()
                                        .setId(value.getId().toString())
                                        .setAdditionalInfoId(orEmpty(value.getSourceAdditionalInfoId()))
                                        .setFieldCode(orEmpty(value.getFieldCode()))
                                        .setFieldName(orEmpty(value.getFieldName()))
                                        .setFieldType(com.simulator112.incident.grpc.contract.FieldType.valueOf(value.getFieldType()))
                                        .setRequired(value.isRequired())
                                        .setFieldValue(orEmpty(value.getFieldValue()))
                                        .build()).toList());

        entity.getDialups().forEach(dialup -> builder.addDialups(toProto(dialup)));

        return builder.build();
    }

    public static com.simulator112.incident.grpc.contract.DialupContext toProto(DialupContextEntity entity) {
        com.simulator112.incident.grpc.contract.DialupDetails.Builder detailsBuilder =
                com.simulator112.incident.grpc.contract.DialupDetails.newBuilder()
                        .addAllKnownFacts(orEmpty(entity.getKnownFacts()))
                        .addAllHiddenFacts(orEmpty(entity.getHiddenFacts()))
                        .setAiContext(orEmpty(entity.getAiContext()))
                        .setEmotionalState(orEmpty(entity.getEmotionalState()));

        if (entity.getGender() != null) {
            detailsBuilder.setGender(com.simulator112.incident.grpc.contract.Gender.valueOf(entity.getGender().name()));
        }

        com.simulator112.incident.grpc.contract.DialupContext.Builder builder =
                com.simulator112.incident.grpc.contract.DialupContext.newBuilder()
                        .setId(entity.getSourceDialupId().toString())
                        .setPosition(entity.getPosition())
                        .setApplicant(toProto(entity.getApplicant()))
                        .setVictim(toProto(entity.getStage().getVictim()))
                        .setDialupDetails(detailsBuilder.build());

        return builder.build();
    }

    private static com.simulator112.incident.grpc.contract.IncidentTypeInfo toProto(IncidentTypeInfo entity) {
        return com.simulator112.incident.grpc.contract.IncidentTypeInfo.newBuilder()
                .setId(orEmpty(entity.getId()))
                .setTypeId(orEmpty(entity.getTypeId()))
                .setServiceType(com.simulator112.incident.grpc.contract.ServiceType.valueOf(entity.getServiceType().name()))
                .setTypeName(orEmpty(entity.getTypeName()))
                .build();
    }

    private static com.simulator112.incident.grpc.contract.Address toProto(Address entity) {
        return com.simulator112.incident.grpc.contract.Address.newBuilder()
                .setCity(orEmpty(entity.getCity()))
                .setStreet(orEmpty(entity.getStreet()))
                .setHouse(orEmpty(entity.getHouse()))
                .setBuilding(orEmpty(entity.getBuilding()))
                .setApartment(orEmpty(entity.getApartment()))
                .setFloor(entity.getFloor() == null ? 0 : entity.getFloor())
                .build();
    }

    private static com.simulator112.incident.grpc.contract.Applicant toProto(Applicant entity) {
        if (entity == null) {
            return com.simulator112.incident.grpc.contract.Applicant.getDefaultInstance();
        }

        com.simulator112.incident.grpc.contract.Applicant.Builder builder =
                com.simulator112.incident.grpc.contract.Applicant.newBuilder()
                        .setFirstName(orEmpty(entity.getFirstName()))
                        .setLastName(orEmpty(entity.getLastName()))
                        .setMiddleName(orEmpty(entity.getMiddleName()))
                        .setAge(entity.getAge() == null ? 0 : entity.getAge())
                        .setPhone(orEmpty(entity.getPhone()))
                        .setContactPhone(orEmpty(entity.getContactPhone()))
                        .setAddress(orEmpty(entity.getAddress()))
                        .setAdditionalInfo(orEmpty(entity.getAdditionalInfo()));
        if (entity.getEmotionalState() != null) {
            builder.setEmotionalState(com.simulator112.incident.grpc.contract.EmotionalState.valueOf(
                    entity.getEmotionalState().name()));
        }
        return builder.build();
    }

    private static com.simulator112.incident.grpc.contract.DispatcherCriteria toProto(DispatcherCriteria entity) {
        return com.simulator112.incident.grpc.contract.DispatcherCriteria.newBuilder()
                .addAllRequiredQuestions(orEmpty(entity.getRequiredQuestions()))
                .addAllExpectedActions(orEmpty(entity.getExpectedActions()))
                .addAllCriticalMistakes(orEmpty(entity.getCriticalMistakes()))
                .build();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> java.util.List<T> orEmpty(java.util.List<T> value) {
        return value == null ? java.util.List.of() : value;
    }
}
