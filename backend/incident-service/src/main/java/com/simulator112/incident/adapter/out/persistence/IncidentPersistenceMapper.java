package com.simulator112.incident.adapter.out.persistence;

import com.simulator112.incident.adapter.out.persistence.entity.common.*;
import com.simulator112.incident.adapter.out.persistence.entity.dds.DdsStageDetailsJpaEntity;
import com.simulator112.incident.adapter.out.persistence.entity.dds.DdsStageTransitionEmbeddable;
import com.simulator112.incident.adapter.out.persistence.entity.system112.System112StageDetailsJpaEntity;
import com.simulator112.incident.domain.common.*;
import com.simulator112.incident.domain.dds.*;
import com.simulator112.incident.domain.system112.System112Criteria;
import com.simulator112.incident.domain.system112.System112Incident;
import com.simulator112.incident.domain.system112.System112Stage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentPersistenceMapper {

    public Incident toDomain(IncidentJpaEntity entity) {
        if (entity.getTargetType() == com.simulator112.incident.domain.common.IncidentTargetType.DDS) {
            List<DdsStage> stages = entity.getStages().stream().map(this::toDdsStage).toList();
            return new DdsIncident(
                    entity.getId(), entity.getTitle(), toDomain(entity.getAddress()), entity.getDifficulty(), stages,
                    new PreparedCardTemplate(entity.getPreparedCardClassifierCodes(),
                            toDomain(entity.getCardApplicant()), toDomain(entity.getCardVictim()),
                            entity.getPreparedCardAdditionalInfo()),
                    new InitialAssignment(entity.getEmergencyService(), entity.getInitialAssignmentClassifierCode(),
                            entity.getInitialAssignmentInstructions()),
                    new DdsCriteria(entity.getRequiredQuestions(), entity.getExpectedActions(),
                            entity.getCriticalMistakes()),
                    entity.getDdsInitialStageId(),
                    entity.getDdsStageTransitions().stream()
                            .map(value -> new DdsStageTransition(value.getStageId(), value.getSuccessStageId(),
                                    value.getFailureStageId()))
                            .toList());
        }
        List<System112Stage> stages = entity.getStages().stream().map(this::toSystem112Stage).toList();
        return new System112Incident(
                entity.getId(), entity.getTitle(), toDomain(entity.getAddress()), entity.getDifficulty(), stages,
                new System112Criteria(entity.getRequiredQuestions(), entity.getExpectedActions(),
                        entity.getCriticalMistakes()));
    }

    public IncidentJpaEntity toEntity(Incident incident) {
        IncidentJpaEntity entity = new IncidentJpaEntity();
        entity.setId(incident.id());
        entity.setTitle(incident.title());
        entity.setTargetType(incident.targetType());
        entity.setDifficulty(incident.difficulty());
        entity.setAddress(toEntity(incident.address()));

        if (incident instanceof System112Incident system112) {
            system112.stages().stream().map(this::toEntity).forEach(entity::addStage);
            setCriteria(entity, system112.criteria().requiredQuestions(), system112.criteria().expectedActions(),
                    system112.criteria().criticalMistakes());
        } else if (incident instanceof DdsIncident dds) {
            dds.stages().stream().map(this::toEntity).forEach(entity::addStage);
            setCriteria(entity, dds.criteria().requiredQuestions(), dds.criteria().expectedActions(),
                    dds.criteria().criticalMistakes());
            PreparedCardTemplate card = dds.preparedCardTemplate();
            entity.setPreparedCardClassifierCodes(new java.util.ArrayList<>(card.classifierCodes()));
            entity.setCardApplicant(toEntity(card.applicant()));
            entity.setCardVictim(toEntity(card.victim()));
            entity.setPreparedCardAdditionalInfo(new java.util.LinkedHashMap<>(card.additionalInfo()));
            InitialAssignment assignment = dds.initialAssignment();
            entity.setEmergencyService(assignment.emergencyService());
            entity.setInitialAssignmentClassifierCode(assignment.classifierCode());
            entity.setInitialAssignmentInstructions(assignment.instructions());
            entity.setDdsInitialStageId(dds.initialStageId());
            entity.setDdsStageTransitions(dds.transitions().stream()
                    .map(value -> new DdsStageTransitionEmbeddable(
                            value.stageId(), value.successStageId(), value.failureStageId()))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        }
        return entity;
    }

    private System112Stage toSystem112Stage(IncidentStageJpaEntity entity) {
        System112StageDetailsJpaEntity details = entity.getSystem112Details();
        return new System112Stage(
                entity.getId(), entity.getTitle(), entity.getPosition(), details.getClassifierCodes(),
                toDomain(details.getVictim()), entity.getDescription(),
                entity.getCalls().stream().map(this::toDomain).toList());
    }

    private DdsStage toDdsStage(IncidentStageJpaEntity entity) {
        DdsStageDetailsJpaEntity details = entity.getDdsDetails();
        return new DdsStage(
                entity.getId(), entity.getTitle(), entity.getDescription(), details.getType(),
                details.getTimeLimitSeconds(), entity.getCalls().stream().map(this::toDomain).toList());
    }

    private IncidentStageJpaEntity toEntity(System112Stage stage) {
        IncidentStageJpaEntity entity = toEntityBase(stage);
        entity.setPosition(stage.position());
        System112StageDetailsJpaEntity details = new System112StageDetailsJpaEntity();
        details.setClassifierCodes(new java.util.ArrayList<>(stage.classifierCodes()));
        details.setVictim(toEntity(stage.victim()));
        entity.setSystem112Details(details);
        return entity;
    }

    private IncidentStageJpaEntity toEntity(DdsStage stage) {
        IncidentStageJpaEntity entity = toEntityBase(stage);
        DdsStageDetailsJpaEntity details = new DdsStageDetailsJpaEntity();
        details.setType(stage.type());
        details.setTimeLimitSeconds(stage.timeLimitSeconds());
        entity.setDdsDetails(details);
        return entity;
    }

    private IncidentStageJpaEntity toEntityBase(IncidentStage stage) {
        IncidentStageJpaEntity entity = new IncidentStageJpaEntity();
        entity.setId(stage.id() == null ? java.util.UUID.randomUUID() : stage.id());
        entity.setTitle(stage.title());
        entity.setDescription(stage.description());
        stage.calls().stream().map(this::toEntity).forEach(entity::addCall);
        return entity;
    }

    private CallScenario toDomain(CallScenarioJpaEntity entity) {
        return new CallScenario(
                entity.getId(), entity.getPosition(), entity.getDirection(), entity.getCounterparty(),
                toDomain(entity.getPerson()), entity.getGender(), entity.getKnownFacts(), entity.getHiddenFacts(),
                entity.getAiContext(), entity.getEmotionalState());
    }

    private CallScenarioJpaEntity toEntity(CallScenario call) {
        CallScenarioJpaEntity entity = new CallScenarioJpaEntity();
        entity.setId(call.id());
        entity.setPosition(call.position());
        entity.setDirection(call.direction());
        entity.setCounterparty(call.counterparty());
        entity.setPerson(toEntity(call.person()));
        entity.setGender(call.gender());
        entity.setKnownFacts(new java.util.ArrayList<>(call.knownFacts()));
        entity.setHiddenFacts(new java.util.ArrayList<>(call.hiddenFacts()));
        entity.setAiContext(call.aiContext());
        entity.setEmotionalState(call.emotionalState());
        return entity;
    }

    private void setCriteria(IncidentJpaEntity entity, List<String> questions, List<String> actions,
                             List<String> mistakes) {
        entity.setRequiredQuestions(new java.util.ArrayList<>(questions));
        entity.setExpectedActions(new java.util.ArrayList<>(actions));
        entity.setCriticalMistakes(new java.util.ArrayList<>(mistakes));
    }

    private Address toDomain(AddressEmbeddable value) {
        return new Address(value.getCity(), value.getStreet(), value.getHouse(), value.getBuilding(),
                value.getApartment(), value.getFloor());
    }

    private AddressEmbeddable toEntity(Address value) {
        return new AddressEmbeddable(value.city(), value.street(), value.house(), value.building(),
                value.apartment(), value.floor());
    }

    private Person toDomain(PersonEmbeddable value) {
        return value == null ? null : new Person(value.getFirstName(), value.getLastName(), value.getMiddleName(),
                value.getAge(), value.getPhone(), value.getContactPhone(), value.getAddress(),
                value.getAdditionalInfo());
    }

    private PersonEmbeddable toEntity(Person value) {
        return value == null ? null : new PersonEmbeddable(value.firstName(), value.lastName(), value.middleName(),
                value.age(), value.phone(), value.contactPhone(), value.address(), value.additionalInfo());
    }
}
