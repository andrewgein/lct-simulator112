package com.simulator112.contextmanager.adapter.grpc.mapper;

import com.simulator112.contextmanager.domain.common.Address;
import com.simulator112.contextmanager.domain.common.CallDirection;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.CounterpartyType;
import com.simulator112.contextmanager.domain.common.Criteria;
import com.simulator112.contextmanager.domain.common.DialogueCriterion;
import com.simulator112.contextmanager.domain.common.Gender;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.Person;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.contextmanager.domain.dds.DdsStageDetails;
import com.simulator112.contextmanager.domain.system112.System112StageDetails;
import com.simulator112.contextmanager.domain.dds.DdsStageType;
import com.simulator112.incident.grpc.contract.CallScenario;
import com.simulator112.incident.grpc.contract.IncidentContext;
import com.simulator112.incident.grpc.contract.IncidentStage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

public final class IncidentContextMapper {
    private IncidentContextMapper() {}

    public static IncidentSnapshot toDomain(IncidentContext proto, int position) {
        IncidentSnapshot value = new IncidentSnapshot();
        value.setSourceId(UUID.fromString(proto.getId())); value.setPosition(position); value.setTitle(proto.getTitle());
        if (proto.getTargetType() != com.simulator112.incident.grpc.contract.IncidentTargetType.INCIDENT_TARGET_TYPE_UNSPECIFIED) {
            value.setTargetType(IncidentTargetType.valueOf(proto.getTargetType().name().replace("INCIDENT_TARGET_TYPE_", "")));
        }
        if (proto.getDifficulty() != com.simulator112.incident.grpc.contract.Difficulty.DIFFICULTY_UNSPECIFIED) {
            value.setDifficulty(com.simulator112.shared.dto.Difficulty.valueOf(proto.getDifficulty().name().replace("DIFFICULTY_", "")));
        }
        value.setAddress(toDomain(proto.getAddress())); value.setCriteria(toDomain(proto.getCriteria()));
        value.setStatus(IncidentProgressStatus.PENDING);
        if (proto.hasPreparedCardTemplate()) {
            value.setPreparedCardClassifierCodes(new ArrayList<>(proto.getPreparedCardTemplate().getClassifierCodesList()));
            value.setCardApplicant(toDomain(proto.getPreparedCardTemplate().getApplicant()));
            value.setCardVictimCount(proto.getPreparedCardTemplate().getVictimCount());
            value.setPreparedCardAdditionalInfo(new LinkedHashMap<>(proto.getPreparedCardTemplate().getAdditionalInfoMap()));
        }
        if (proto.hasInitialAssignment()) {
            value.setInitialAssignmentService(proto.getInitialAssignment().getEmergencyServiceCode());
            value.setInitialAssignmentClassifierCode(proto.getInitialAssignment().getClassifierCode());
            value.setInitialAssignmentInstructions(proto.getInitialAssignment().getInstructions());
        }
        value.setStages(proto.getStagesList().stream().map(IncidentContextMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        if (value.getTargetType() == IncidentTargetType.DDS) {
            for (int index = 0; index < value.getStages().size(); index++) value.getStages().get(index).setPosition(index);
        }
        return value;
    }

    private static StageSnapshot toDomain(IncidentStage proto) {
        StageSnapshot value = new StageSnapshot();
        value.setSourceId(UUID.fromString(proto.getId())); value.setTitle(proto.getTitle());
        value.setDescription(proto.getDescription()); value.setStatus(StageStatus.PENDING);
        if (proto.hasSystem112()) {
            value.setPosition(proto.getSystem112().getPosition());
            value.setSystem112(new System112StageDetails(proto.getSystem112().getClassifierCodesList(),
                    proto.getSystem112().getVictimCount()));
        } else if (proto.hasDds()) {
            value.setDds(new DdsStageDetails(DdsStageType.valueOf(proto.getDds().getType().name().replace("DDS_STAGE_TYPE_", "")),
                    proto.getDds().getTimeLimitSeconds(), proto.getDds().getExpectedComment(), null,
                    proto.getDds().getActualStatus() == com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_UNSPECIFIED
                            ? null : com.simulator112.contextmanager.domain.common.IncidentStatus.valueOf(
                                    proto.getDds().getActualStatus().name().replace("INCIDENT_STATUS_", ""))));
        }
        value.setCalls(proto.getCallsList().stream().map(IncidentContextMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return value;
    }

    private static CallSnapshot toDomain(CallScenario proto) {
        CallSnapshot value = new CallSnapshot();
        value.setSourceId(UUID.fromString(proto.getId())); value.setPosition(proto.getPosition());
        value.setDirection(CallDirection.valueOf(proto.getDirection().name().replace("CALL_DIRECTION_", "")));
        value.setCounterparty(CounterpartyType.valueOf(proto.getCounterparty().name().replace("COUNTERPARTY_TYPE_", "")));
        value.setStatus(CallStatus.PENDING); value.setApplicant(toDomain(proto.getPerson()));
        if (proto.getGender() != com.simulator112.incident.grpc.contract.Gender.GENDER_UNSPECIFIED) {
            value.setGender(Gender.valueOf(proto.getGender().name().replace("GENDER_", "")));
        }
        value.setKnownFacts(new ArrayList<>(proto.getKnownFactsList())); value.setHiddenFacts(new ArrayList<>(proto.getHiddenFactsList()));
        value.setAiContext(proto.getAiContext()); value.setEmotionalState(proto.getEmotionalState());
        return value;
    }

    public static IncidentContext toProto(IncidentSnapshot value) {
        var builder = IncidentContext.newBuilder().setId(value.getSourceId().toString()).setTitle(orEmpty(value.getTitle()))
                .setAddress(toProto(value.getAddress())).setCriteria(toProto(value.getCriteria()))
                .setTargetType(com.simulator112.incident.grpc.contract.IncidentTargetType.valueOf("INCIDENT_TARGET_TYPE_" + value.getTargetType().name()))
                .setDifficulty(com.simulator112.incident.grpc.contract.Difficulty.valueOf("DIFFICULTY_" + value.getDifficulty().name()));
        value.getStages().forEach(stage -> builder.addStages(toProto(stage)));
        if (!value.getPreparedCardClassifierCodes().isEmpty()) builder.setPreparedCardTemplate(
                com.simulator112.incident.grpc.contract.PreparedCardTemplate.newBuilder()
                        .addAllClassifierCodes(value.getPreparedCardClassifierCodes()).setApplicant(toProto(value.getCardApplicant()))
                        .setVictimCount(value.getCardVictimCount()).putAllAdditionalInfo(value.getPreparedCardAdditionalInfo()));
        if (value.getInitialAssignmentService() != null) builder.setInitialAssignment(
                com.simulator112.incident.grpc.contract.InitialAssignment.newBuilder()
                        .setEmergencyServiceCode(value.getInitialAssignmentService())
                        .setClassifierCode(orEmpty(value.getInitialAssignmentClassifierCode())).setInstructions(orEmpty(value.getInitialAssignmentInstructions())));
        return builder.build();
    }

    private static IncidentStage toProto(StageSnapshot value) {
        var builder = IncidentStage.newBuilder().setId(value.getSourceId().toString()).setTitle(orEmpty(value.getTitle()))
                .setDescription(orEmpty(value.getDescription())).addAllCalls(value.getCalls().stream().map(IncidentContextMapper::toProto).toList());
        if (value.getSystem112() != null) builder.setSystem112(
                com.simulator112.incident.grpc.contract.System112StageDetails.newBuilder().setPosition(value.getPosition())
                        .addAllClassifierCodes(value.getSystem112().classifierCodes()).setVictimCount(value.getSystem112().victimCount()));
        else if (value.getDds() != null) builder.setDds(com.simulator112.incident.grpc.contract.DdsStageDetails.newBuilder()
                .setType(com.simulator112.incident.grpc.contract.DdsStageType.valueOf("DDS_STAGE_TYPE_" + value.getDds().getType().name()))
                .setTimeLimitSeconds(value.getDds().getTimeLimitSeconds())
                .setExpectedComment(orEmpty(value.getDds().getExpectedComment()))
                .setActualStatus(value.getDds().getActualStatus() == null
                        ? com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_UNSPECIFIED
                        : com.simulator112.incident.grpc.contract.IncidentStatus.valueOf(
                                "INCIDENT_STATUS_" + value.getDds().getActualStatus().name())));
        else throw new IllegalStateException("Не указан тип этапа: " + value.getSourceId());
        return builder.build();
    }

    public static CallScenario toProto(CallSnapshot value) {
        var builder = CallScenario.newBuilder().setId(value.getSourceId().toString()).setPosition(value.getPosition())
                .setDirection(com.simulator112.incident.grpc.contract.CallDirection.valueOf("CALL_DIRECTION_" + value.getDirection().name()))
                .setCounterparty(com.simulator112.incident.grpc.contract.CounterpartyType.valueOf("COUNTERPARTY_TYPE_" + value.getCounterparty().name()))
                .setPerson(toProto(value.getApplicant())).addAllKnownFacts(value.getKnownFacts()).addAllHiddenFacts(value.getHiddenFacts())
                .setAiContext(orEmpty(value.getAiContext())).setEmotionalState(orEmpty(value.getEmotionalState()));
        if (value.getGender() != null) builder.setGender(com.simulator112.incident.grpc.contract.Gender.valueOf("GENDER_" + value.getGender().name()));
        return builder.build();
    }

    private static Criteria toDomain(com.simulator112.incident.grpc.contract.Criteria value) {
        return new Criteria(value.getDialogueCriteriaList().stream().map(criterion -> new DialogueCriterion(
                java.util.UUID.fromString(criterion.getId()), criterion.getName(),
                criterion.getHypothesis(), criterion.getWeight())).toList());
    }
    private static com.simulator112.incident.grpc.contract.Criteria toProto(Criteria value) {
        return com.simulator112.incident.grpc.contract.Criteria.newBuilder()
                .addAllDialogueCriteria(value.dialogueCriteria().stream().map(criterion ->
                        com.simulator112.incident.grpc.contract.DialogueCriterion.newBuilder()
                                .setId(criterion.id().toString())
                                .setName(criterion.name())
                                .setHypothesis(criterion.hypothesis())
                                .setWeight(criterion.weight())
                                .build()).toList())
                .build();
    }
    private static Address toDomain(com.simulator112.incident.grpc.contract.Address value) {
        return new Address(value.getCity(), value.getStreet(), value.getHouse(), value.getBuilding(), value.getApartment(), value.hasFloor() ? value.getFloor() : null);
    }
    private static com.simulator112.incident.grpc.contract.Address toProto(Address value) {
        var builder = com.simulator112.incident.grpc.contract.Address.newBuilder().setCity(orEmpty(value.city())).setStreet(orEmpty(value.street()))
                .setHouse(orEmpty(value.house())).setBuilding(orEmpty(value.building())).setApartment(orEmpty(value.apartment()));
        if (value.floor() != null) builder.setFloor(value.floor()); return builder.build();
    }
    private static Person toDomain(com.simulator112.incident.grpc.contract.Person value) {
        if (value.equals(com.simulator112.incident.grpc.contract.Person.getDefaultInstance())) return null;
        return new Person(value.getFirstName(), value.getLastName(), value.getMiddleName(), value.hasAge() ? value.getAge() : null,
                value.getPhone(), value.getContactPhone(), value.getOnScenePhone(), value.getAddress(),
                value.getAdditionalInfo(), null);
    }
    private static com.simulator112.incident.grpc.contract.Person toProto(Person value) {
        if (value == null) return com.simulator112.incident.grpc.contract.Person.getDefaultInstance();
        var builder = com.simulator112.incident.grpc.contract.Person.newBuilder().setFirstName(orEmpty(value.firstName()))
                .setLastName(orEmpty(value.lastName())).setMiddleName(orEmpty(value.middleName())).setPhone(orEmpty(value.phone()))
                .setContactPhone(orEmpty(value.contactPhone())).setOnScenePhone(orEmpty(value.onScenePhone()))
                .setAddress(orEmpty(value.address())).setAdditionalInfo(orEmpty(value.additionalInfo()));
        if (value.age() != null) builder.setAge(value.age()); return builder.build();
    }
    private static UUID uuid(String value) { return value == null || value.isBlank() ? null : UUID.fromString(value); }
    private static String orEmpty(Object value) { return value == null ? "" : value.toString(); }
}
