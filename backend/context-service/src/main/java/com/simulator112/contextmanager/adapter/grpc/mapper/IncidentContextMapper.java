package com.simulator112.contextmanager.adapter.grpc.mapper;

import com.simulator112.contextmanager.domain.common.Address;
import com.simulator112.contextmanager.domain.common.CallDirection;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.CounterpartyType;
import com.simulator112.contextmanager.domain.common.Criteria;
import com.simulator112.contextmanager.domain.common.Gender;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.contextmanager.domain.common.Person;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.StageStatus;
import com.simulator112.contextmanager.domain.dds.DdsStageTransition;
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
            value.setInitialAssignmentService(proto.getInitialAssignment().getEmergencyService().name());
            value.setInitialAssignmentClassifierCode(proto.getInitialAssignment().getClassifierCode());
            value.setInitialAssignmentInstructions(proto.getInitialAssignment().getInstructions());
        }
        if (!proto.getDdsInitialStageId().isBlank()) value.setInitialStageId(UUID.fromString(proto.getDdsInitialStageId()));
        value.setTransitions(proto.getDdsStageTransitionsList().stream().map(item -> new DdsStageTransition(
                UUID.fromString(item.getStageId()), uuid(item.getSuccessStageId()), uuid(item.getFailureStageId())))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        value.setStages(proto.getStagesList().stream().map(IncidentContextMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return value;
    }

    private static StageSnapshot toDomain(IncidentStage proto) {
        StageSnapshot value = new StageSnapshot();
        value.setSourceId(UUID.fromString(proto.getId())); value.setTitle(proto.getTitle());
        value.setDescription(proto.getDescription()); value.setStatus(StageStatus.PENDING);
        if (proto.hasSystem112()) {
            value.setPosition(proto.getSystem112().getPosition()); value.setClassifierCodes(new ArrayList<>(proto.getSystem112().getClassifierCodesList()));
            value.setVictimCount(proto.getSystem112().getVictimCount());
        } else if (proto.hasDds()) {
            value.setDdsStageType(DdsStageType.valueOf(proto.getDds().getType().name().replace("DDS_STAGE_TYPE_", "")));
            value.setTimeLimitSeconds(proto.getDds().getTimeLimitSeconds());
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
                        .setEmergencyService(com.simulator112.common.grpc.contract.DdsService.valueOf(value.getInitialAssignmentService()))
                        .setClassifierCode(orEmpty(value.getInitialAssignmentClassifierCode())).setInstructions(orEmpty(value.getInitialAssignmentInstructions())));
        if (value.getInitialStageId() != null) builder.setDdsInitialStageId(value.getInitialStageId().toString());
        value.getTransitions().forEach(item -> builder.addDdsStageTransitions(
                com.simulator112.incident.grpc.contract.DdsStageTransition.newBuilder().setStageId(item.stageId().toString())
                        .setSuccessStageId(orEmpty(item.successStageId())).setFailureStageId(orEmpty(item.failureStageId()))));
        return builder.build();
    }

    private static IncidentStage toProto(StageSnapshot value) {
        var builder = IncidentStage.newBuilder().setId(value.getSourceId().toString()).setTitle(orEmpty(value.getTitle()))
                .setDescription(orEmpty(value.getDescription())).addAllCalls(value.getCalls().stream().map(IncidentContextMapper::toProto).toList());
        if (value.getDdsStageType() == null) builder.setSystem112(
                com.simulator112.incident.grpc.contract.System112StageDetails.newBuilder().setPosition(value.getPosition())
                        .addAllClassifierCodes(value.getClassifierCodes()).setVictimCount(value.getVictimCount()));
        else builder.setDds(com.simulator112.incident.grpc.contract.DdsStageDetails.newBuilder()
                .setType(com.simulator112.incident.grpc.contract.DdsStageType.valueOf("DDS_STAGE_TYPE_" + value.getDdsStageType().name()))
                .setTimeLimitSeconds(value.getTimeLimitSeconds()));
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
        return new Criteria(value.getRequiredQuestionsList(), value.getExpectedActionsList(), value.getCriticalMistakesList());
    }
    private static com.simulator112.incident.grpc.contract.Criteria toProto(Criteria value) {
        return com.simulator112.incident.grpc.contract.Criteria.newBuilder().addAllRequiredQuestions(value.requiredQuestions())
                .addAllExpectedActions(value.expectedActions()).addAllCriticalMistakes(value.criticalMistakes()).build();
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
