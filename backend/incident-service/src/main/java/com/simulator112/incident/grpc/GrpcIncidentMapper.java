package com.simulator112.incident.grpc;

import com.simulator112.incident.grpc.contract.Address;
import com.simulator112.incident.grpc.contract.Applicant;
import com.simulator112.incident.grpc.contract.Difficulty;
import com.simulator112.incident.grpc.contract.DispatcherCriteria;
import com.simulator112.incident.grpc.contract.IncidentAdditionalInfo;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.DialupDetails;
import com.simulator112.incident.grpc.contract.IncidentTypeInfo;
import com.simulator112.incident.grpc.contract.LevelContext;
import com.simulator112.incident.grpc.contract.StageContext;
import com.simulator112.incident.grpc.contract.DialupContext;
import com.simulator112.incident.model.entity.IncidentEntity;
import com.simulator112.incident.model.entity.LevelEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GrpcIncidentMapper {

    public LevelContext toLevelContext(LevelEntity entity) {
        return LevelContext.newBuilder()
                .setId(entity.getId().toString())
                .setTitle(nullToEmpty(entity.getTitle()))
                .setDifficulty(Difficulty.valueOf(entity.getDifficulty().name()))
                .addAllIncidents(entity.getIncidents().stream().map(this::toIncidentContext).toList())
                .build();
    }

    public IncidentContext toIncidentContext(IncidentEntity entity) {
        return IncidentContext.newBuilder()
                .setId(entity.getId().toString())
                .setTitle(nullToEmpty(entity.getTitle()))
                .setAddress(toAddress(entity.getAddress()))
                .setDispatcherCriteria(toDispatcherCriteria(entity.getCriteria()))
                .addAllStages(entity.getStages().stream().map(stage -> StageContext.newBuilder()
                        .setId(stage.getId().toString())
                        .setTitle(nullToEmpty(stage.getTitle()))
                        .setPosition(stage.getPosition())
                        .setType(toIncidentType(stage.getType()))
                        .setDescription(nullToEmpty(stage.getDescription()))
                        .setVictim(toApplicant(stage.getVictim()))
                        .addAllAdditionalInfo(stage.getAdditionalInfo().stream().map(value -> toAdditionalInfo(
                                value.getId(), value.getAdditionalInfo(), value.getFieldValue())).toList())
                        .addAllDialups(stage.getDialups().stream().map(this::toDialupContext).toList())
                        .build()).toList())
                .build();
    }

    private DialupContext toDialupContext(com.simulator112.incident.model.entity.DialupEntity dialup) {
        DialupContext.Builder builder = DialupContext.newBuilder()
                .setId(dialup.getId().toString())
                .setPosition(dialup.getPosition())
                .setApplicant(toApplicant(dialup.getApplicant()))
                .setDialupDetails(toDialupDetails(dialup.getDialupDetails()));

        return builder.build();
    }

    private Address toAddress(com.simulator112.incident.model.embeddable.Address address) {

        Address.Builder builder = Address.newBuilder()
                .setCity(nullToEmpty(address.getCity()))
                .setStreet(nullToEmpty(address.getStreet()))
                .setHouse(nullToEmpty(address.getHouse()))
                .setBuilding(nullToEmpty(address.getBuilding()))
                .setApartment(nullToEmpty(address.getApartment()));

        if (address.getFloor() != null) {
            builder.setFloor(address.getFloor());
        }

        return builder.build();
    }

    private Applicant toApplicant(com.simulator112.incident.model.embeddable.Applicant applicant) {
        if (applicant == null) {
            return Applicant.getDefaultInstance();
        }

        Applicant.Builder builder = Applicant.newBuilder()
                .setFirstName(nullToEmpty(applicant.getFirstName()))
                .setLastName(nullToEmpty(applicant.getLastName()))
                .setMiddleName(nullToEmpty(applicant.getMiddleName()))
                .setPhone(nullToEmpty(applicant.getPhone()))
                .setContactPhone(nullToEmpty(applicant.getContactPhone()))
                .setAddress(nullToEmpty(applicant.getAddress()))
                .setAdditionalInfo(nullToEmpty(applicant.getAdditionalInfo()));

        if (applicant.getAge() != null) {
            builder.setAge(applicant.getAge());
        }

        return builder.build();
    }

    private DialupDetails toDialupDetails(com.simulator112.incident.model.embeddable.DialupDetails details) {
        DialupDetails.Builder builder = DialupDetails.newBuilder()
                .addAllKnownFacts(nullToEmptyList(details.getKnownFacts()))
                .addAllHiddenFacts(nullToEmptyList(details.getHiddenFacts()))
                .setAiContext(nullToEmpty(details.getAiContext()))
                .setEmotionalState(nullToEmpty(details.getEmotionalState()));

        if (details.getGender() != null) {
            builder.setGender(com.simulator112.incident.grpc.contract.Gender.valueOf(details.getGender().name()));
        }

        return builder.build();
    }

    private DispatcherCriteria toDispatcherCriteria(com.simulator112.incident.model.embeddable.DispatcherCriteria criteria) {
        return DispatcherCriteria.newBuilder()
                .addAllRequiredQuestions(nullToEmptyList(criteria.getRequiredQuestions()))
                .addAllExpectedActions(nullToEmptyList(criteria.getExpectedActions()))
                .addAllCriticalMistakes(nullToEmptyList(criteria.getCriticalMistakes()))
                .build();
    }

    private IncidentTypeInfo toIncidentType(com.simulator112.incident.model.entity.TypeEntity type) {
        return IncidentTypeInfo.newBuilder()
                .setId(type.getId().toString())
                .setTypeId(type.getTypeId())
                .setServiceType(com.simulator112.incident.grpc.contract.ServiceType.valueOf(type.getServiceType().name()))
                .setTypeName(type.getTypeName())
                .build();
    }

    private IncidentAdditionalInfo toAdditionalInfo(java.util.UUID id,
                                                     com.simulator112.incident.model.entity.AdditionalInfoEntity field,
                                                     String value) {
        return IncidentAdditionalInfo.newBuilder()
                .setId(id.toString())
                .setAdditionalInfoId(field.getId().toString())
                .setFieldCode(field.getFieldCode())
                .setFieldName(field.getFieldName())
                .setFieldType(com.simulator112.incident.grpc.contract.FieldType.valueOf(field.getFieldType().name()))
                .setRequired(field.isRequired())
                .setFieldValue(nullToEmpty(value))
                .build();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> nullToEmptyList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
