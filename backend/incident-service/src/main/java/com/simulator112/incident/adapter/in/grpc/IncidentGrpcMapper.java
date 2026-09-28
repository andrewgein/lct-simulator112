package com.simulator112.incident.adapter.in.grpc;

import com.simulator112.incident.domain.common.CallScenario;
import com.simulator112.incident.domain.common.DialogueCriterion;
import com.simulator112.incident.domain.common.Incident;
import com.simulator112.incident.domain.common.IncidentStage;
import com.simulator112.incident.domain.common.Person;
import com.simulator112.incident.domain.dds.DdsIncident;
import com.simulator112.incident.domain.dds.DdsStage;
import com.simulator112.incident.domain.system112.System112Incident;
import com.simulator112.incident.domain.system112.System112Stage;
import com.simulator112.incident.grpc.contract.IncidentContext;
import org.springframework.stereotype.Component;

@Component
public class IncidentGrpcMapper {
    public IncidentContext toProto(Incident incident) {
        IncidentContext.Builder builder = IncidentContext.newBuilder()
                .setId(incident.id().toString())
                .setTitle(incident.title())
                .setAddress(toProto(incident.address()))
                .setDifficulty(toProto(incident.difficulty()))
                .setTargetType(toProto(incident.targetType()));

        if (incident instanceof System112Incident system112) {
            builder.addAllStages(system112.stages().stream().map(this::toProto).toList());
            builder.setCriteria(criteria(system112.criteria().dialogueCriteria()));
        } else if (incident instanceof DdsIncident dds) {
            builder.addAllStages(dds.stages().stream().map(this::toProto).toList());
            builder.setPreparedCardTemplate(toProto(dds.preparedCardTemplate()));
            builder.setInitialAssignment(toProto(dds.initialAssignment()));
        }
        return builder.build();
    }

    private com.simulator112.incident.grpc.contract.IncidentStage toProto(System112Stage stage) {
        return toProtoBase(stage)
                .setSystem112(com.simulator112.incident.grpc.contract.System112StageDetails.newBuilder()
                        .addAllClassifierCodes(stage.classifierCodes())
                        .setVictimCount(stage.victimCount())
                        .setPosition(stage.position()))
                .build();
    }

    private com.simulator112.incident.grpc.contract.IncidentStage toProto(DdsStage stage) {
        return toProtoBase(stage)
                .setDds(com.simulator112.incident.grpc.contract.DdsStageDetails.newBuilder()
                        .setType(toProto(stage.type()))
                        .setTimeLimitSeconds(stage.timeLimitSeconds())
                        .setExpectedComment(string(stage.expectedComment()))
                        .setActualStatus(stage.actualStatus() == null
                                ? com.simulator112.incident.grpc.contract.IncidentStatus.INCIDENT_STATUS_UNSPECIFIED
                                : com.simulator112.incident.grpc.contract.IncidentStatus.valueOf("INCIDENT_STATUS_" + stage.actualStatus().name())))
                .build();
    }

    private com.simulator112.incident.grpc.contract.IncidentStage.Builder toProtoBase(IncidentStage stage) {
        return com.simulator112.incident.grpc.contract.IncidentStage.newBuilder()
                .setId(string(stage.id()))
                .setTitle(string(stage.title()))
                .setDescription(string(stage.description()))
                .addAllCalls(stage.calls().stream().map(this::toProto).toList());
    }

    private com.simulator112.incident.grpc.contract.DdsStageType toProto(
            com.simulator112.incident.domain.dds.DdsStageType value) {
        return switch (value) {
            case ASSIGN_BRIGADE -> com.simulator112.incident.grpc.contract.DdsStageType.DDS_STAGE_TYPE_ASSIGN_BRIGADE;
            case WAIT_FOR_BRIGADE_STATUS_CHANGE ->
                    com.simulator112.incident.grpc.contract.DdsStageType.DDS_STAGE_TYPE_WAIT_FOR_BRIGADE_STATUS_CHANGE;
            case CALL_BRIGADE_FOR_STATUS ->
                    com.simulator112.incident.grpc.contract.DdsStageType.DDS_STAGE_TYPE_CALL_BRIGADE_FOR_STATUS;
            case REQUEST_ADDITIONAL_SERVICE ->
                    com.simulator112.incident.grpc.contract.DdsStageType.DDS_STAGE_TYPE_REQUEST_ADDITIONAL_SERVICE;
            case COMPLETE_INCIDENT ->
                    com.simulator112.incident.grpc.contract.DdsStageType.DDS_STAGE_TYPE_COMPLETE_INCIDENT;
        };
    }

    private com.simulator112.incident.grpc.contract.CallScenario toProto(CallScenario call) {
        return com.simulator112.incident.grpc.contract.CallScenario.newBuilder()
                .setId(string(call.id()))
                .setPosition(call.position())
                .setDirection(switch (call.direction()) {
                    case INBOUND -> com.simulator112.incident.grpc.contract.CallDirection.CALL_DIRECTION_INBOUND;
                    case OUTBOUND -> com.simulator112.incident.grpc.contract.CallDirection.CALL_DIRECTION_OUTBOUND;
                })
                .setCounterparty(switch (call.counterparty()) {
                    case CALLER -> com.simulator112.incident.grpc.contract.CounterpartyType.COUNTERPARTY_TYPE_CALLER;
                    case BRIGADE -> com.simulator112.incident.grpc.contract.CounterpartyType.COUNTERPARTY_TYPE_BRIGADE;
                    case SERVICE -> com.simulator112.incident.grpc.contract.CounterpartyType.COUNTERPARTY_TYPE_SERVICE;
                })
                .setPerson(toProto(call.person()))
                .setGender(toProto(call.gender()))
                .addAllKnownFacts(call.knownFacts())
                .addAllHiddenFacts(call.hiddenFacts())
                .setAiContext(string(call.aiContext()))
                .setEmotionalState(string(call.emotionalState()))
                .setServiceCode(string(call.serviceCode()))
                .build();
    }

    private com.simulator112.incident.grpc.contract.PreparedCardTemplate toProto(
            com.simulator112.incident.domain.dds.PreparedCardTemplate value) {
        return com.simulator112.incident.grpc.contract.PreparedCardTemplate.newBuilder()
                .addAllClassifierCodes(value.classifierCodes()).setApplicant(toProto(value.applicant()))
                .setVictimCount(value.victimCount()).putAllAdditionalInfo(value.additionalInfo()).build();
    }

    private com.simulator112.incident.grpc.contract.InitialAssignment toProto(
            com.simulator112.incident.domain.dds.InitialAssignment value) {
        return com.simulator112.incident.grpc.contract.InitialAssignment.newBuilder()
                .setEmergencyServiceCode(string(value.emergencyService())).build();
    }

    private com.simulator112.incident.grpc.contract.Criteria criteria(
            java.util.List<DialogueCriterion> criteria) {
        return com.simulator112.incident.grpc.contract.Criteria.newBuilder()
                .addAllDialogueCriteria(criteria.stream().map(value ->
                        com.simulator112.incident.grpc.contract.DialogueCriterion.newBuilder()
                                .setId(value.id().toString())
                                .setName(value.name())
                                .setHypothesis(value.hypothesis())
                                .setWeight(value.weight())
                                .build()).toList())
                .build();
    }

    private com.simulator112.incident.grpc.contract.Address toProto(
            com.simulator112.incident.domain.common.Address value) {
        var builder = com.simulator112.incident.grpc.contract.Address.newBuilder()
                .setCity(string(value.city())).setStreet(string(value.street())).setHouse(string(value.house()))
                .setBuilding(string(value.building())).setApartment(string(value.apartment()));
        if (value.floor() != null) builder.setFloor(value.floor());
        return builder.build();
    }

    private com.simulator112.incident.grpc.contract.Person toProto(Person value) {
        if (value == null) return com.simulator112.incident.grpc.contract.Person.getDefaultInstance();
        var builder = com.simulator112.incident.grpc.contract.Person.newBuilder()
                .setFirstName(string(value.firstName())).setLastName(string(value.lastName()))
                .setMiddleName(string(value.middleName())).setPhone(string(value.phone()))
                .setContactPhone(string(value.contactPhone())).setOnScenePhone(string(value.onScenePhone()))
                .setAddress(string(value.address()))
                .setAdditionalInfo(string(value.additionalInfo()));
        if (value.age() != null) builder.setAge(value.age());
        return builder.build();
    }

    private com.simulator112.incident.grpc.contract.Difficulty toProto(
            com.simulator112.incident.domain.common.Difficulty value) {
        return switch (value) {
            case EASY -> com.simulator112.incident.grpc.contract.Difficulty.DIFFICULTY_EASY;
            case NORMAL -> com.simulator112.incident.grpc.contract.Difficulty.DIFFICULTY_NORMAL;
            case HARD -> com.simulator112.incident.grpc.contract.Difficulty.DIFFICULTY_HARD;
        };
    }

    private com.simulator112.incident.grpc.contract.IncidentTargetType toProto(
            com.simulator112.incident.domain.common.IncidentTargetType value) {
        return switch (value) {
            case SYSTEM_112 ->
                    com.simulator112.incident.grpc.contract.IncidentTargetType.INCIDENT_TARGET_TYPE_SYSTEM_112;
            case DDS -> com.simulator112.incident.grpc.contract.IncidentTargetType.INCIDENT_TARGET_TYPE_DDS;
        };
    }

    private com.simulator112.incident.grpc.contract.Gender toProto(
            com.simulator112.incident.domain.common.Gender value) {
        if (value == null) return com.simulator112.incident.grpc.contract.Gender.GENDER_UNSPECIFIED;
        return switch (value) {
            case MAN -> com.simulator112.incident.grpc.contract.Gender.GENDER_MAN;
            case WOMEN -> com.simulator112.incident.grpc.contract.Gender.GENDER_WOMEN;
        };
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
