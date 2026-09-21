package com.simulator112.contextmanager.adapter.out.persistence;

import com.simulator112.contextmanager.adapter.out.persistence.entity.CallContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.Context;
import com.simulator112.contextmanager.adapter.out.persistence.entity.DialogContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.IncidentContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.SolutionContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.StageContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Applicant;
import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.DdsStageTransitionSnapshot;
import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.DispatcherCriteria;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.DialogTranscript;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.Phrase;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.dds.DdsStageTransition;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import java.util.ArrayList;
import java.util.HashMap;

final class ContextPersistenceMapper {
    private ContextPersistenceMapper() {}

    static TrainingContext toDomain(Context source) {
        TrainingContext target = new TrainingContext();
        target.setId(source.getUuid()); target.setLevelId(source.getLevelId()); target.setLevelTitle(source.getLevelTitle());
        target.setTargetType(source.getTargetType()); target.setDifficulty(source.getDifficulty());
        target.setExecutionMode(source.getExecutionMode()); target.setUserId(source.getUserId());
        target.setStatus(source.getStatus()); target.setActiveCallId(source.getActiveCallId());
        target.setDialogStatus(source.getDialogStatus()); target.setCreatedAt(source.getCreatedAt()); target.setUpdatedAt(source.getUpdatedAt());
        target.setIncidents(source.getIncidentContexts().stream().map(ContextPersistenceMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        target.setSolutionCards(source.getSolutionContexts().stream().map(ContextPersistenceMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        if (source.getDialogContext() != null) {
            target.setDialog(new DialogTranscript(source.getDialogContext().getTranscript().stream()
                    .map(value -> new Phrase(value.getSpeaker(), value.getText())).toList()));
        }
        return target;
    }

    static Context toEntity(TrainingContext source) {
        Context target = new Context();
        target.setUuid(source.getId()); target.setLevelId(source.getLevelId()); target.setLevelTitle(source.getLevelTitle());
        target.setTargetType(source.getTargetType()); target.setDifficulty(source.getDifficulty());
        target.setExecutionMode(source.getExecutionMode()); target.setUserId(source.getUserId());
        target.setStatus(source.getStatus()); target.setActiveCallId(source.getActiveCallId()); target.setDialogStatus(source.getDialogStatus());
        source.getIncidents().stream().map(ContextPersistenceMapper::toEntity).forEach(target::attachIncidentContext);
        source.getSolutionCards().stream().map(ContextPersistenceMapper::toEntity).forEach(target::attachSolutionContext);
        if (source.getDialog() != null) {
            DialogContextEntity dialog = new DialogContextEntity();
            dialog.setContextId(source.getId());
            dialog.setTranscript(source.getDialog().phrases().stream()
                    .map(value -> new com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Phrase(value.speaker(), value.text()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
            target.attachDialogContext(dialog);
        }
        return target;
    }

    private static IncidentSnapshot toDomain(IncidentContextEntity source) {
        IncidentSnapshot target = new IncidentSnapshot();
        target.setPersistenceId(source.getId()); target.setSourceId(source.getSourceIncidentId()); target.setPosition(source.getPosition());
        target.setTitle(source.getTitle()); target.setTargetType(source.getContext().getTargetType());
        target.setDifficulty(source.getContext().getDifficulty()); target.setStatus(source.getStatus()); target.setActiveStageId(source.getActiveStageId());
        target.setInitialStageId(source.getInitialStageId()); target.setAddress(toDomain(source.getAddress()));
        target.setCriteria(toDomain(source.getDispatcherCriteria())); target.setPreparedCardClassifierCode(source.getPreparedCardClassifierCode());
        target.setCardApplicant(toDomain(source.getCardApplicant())); target.setCardVictim(toDomain(source.getCardVictim()));
        target.setPreparedCardAdditionalInfo(new java.util.LinkedHashMap<>(source.getPreparedCardAdditionalInfo()));
        target.setInitialAssignmentService(source.getInitialAssignmentService());
        target.setInitialAssignmentClassifierCode(source.getInitialAssignmentClassifierCode());
        target.setInitialAssignmentInstructions(source.getInitialAssignmentInstructions());
        target.setTransitions(source.getTransitions().stream().map(value -> new DdsStageTransition(
                value.getStageId(), value.getSuccessStageId(), value.getFailureStageId())).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        target.setStages(source.getStages().stream().map(ContextPersistenceMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return target;
    }

    private static IncidentContextEntity toEntity(IncidentSnapshot source) {
        IncidentContextEntity target = new IncidentContextEntity();
        target.setId(source.getPersistenceId()); target.setSourceIncidentId(source.getSourceId()); target.setPosition(source.getPosition());
        target.setTitle(source.getTitle()); target.setStatus(source.getStatus()); target.setActiveStageId(source.getActiveStageId());
        target.setInitialStageId(source.getInitialStageId()); target.setAddress(toEntity(source.getAddress()));
        target.setDispatcherCriteria(toEntity(source.getCriteria())); target.setPreparedCardClassifierCode(source.getPreparedCardClassifierCode());
        target.setCardApplicant(toEntity(source.getCardApplicant())); target.setCardVictim(toEntity(source.getCardVictim()));
        target.setPreparedCardAdditionalInfo(new java.util.LinkedHashMap<>(source.getPreparedCardAdditionalInfo()));
        target.setInitialAssignmentService(source.getInitialAssignmentService());
        target.setInitialAssignmentClassifierCode(source.getInitialAssignmentClassifierCode());
        target.setInitialAssignmentInstructions(source.getInitialAssignmentInstructions());
        target.setTransitions(source.getTransitions().stream().map(value -> new DdsStageTransitionSnapshot(
                value.stageId(), value.successStageId(), value.failureStageId())).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        source.getStages().stream().map(ContextPersistenceMapper::toEntity).forEach(target::addStage);
        return target;
    }

    private static StageSnapshot toDomain(StageContextEntity source) {
        StageSnapshot target = new StageSnapshot();
        target.setPersistenceId(source.getId()); target.setSourceId(source.getSourceStageId()); target.setPosition(source.getPosition());
        target.setTitle(source.getTitle()); target.setClassifierCode(source.getClassifierCode()); target.setDdsStageType(source.getDdsStageType());
        target.setTimeLimitSeconds(source.getTimeLimitSeconds()); target.setStatus(source.getStatus()); target.setStartedAt(source.getStartedAt());
        target.setDeadlineAt(source.getDeadlineAt()); target.setDescription(source.getDescription()); target.setVictim(toDomain(source.getVictim()));
        target.setCalls(source.getCalls().stream().map(ContextPersistenceMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return target;
    }

    private static StageContextEntity toEntity(StageSnapshot source) {
        StageContextEntity target = new StageContextEntity();
        target.setId(source.getPersistenceId()); target.setSourceStageId(source.getSourceId()); target.setPosition(source.getPosition());
        target.setTitle(source.getTitle()); target.setClassifierCode(source.getClassifierCode()); target.setDdsStageType(source.getDdsStageType());
        target.setTimeLimitSeconds(source.getTimeLimitSeconds()); target.setStatus(source.getStatus()); target.setStartedAt(source.getStartedAt());
        target.setDeadlineAt(source.getDeadlineAt()); target.setDescription(source.getDescription()); target.setVictim(toEntity(source.getVictim()));
        source.getCalls().stream().map(ContextPersistenceMapper::toEntity).forEach(target::addCall);
        return target;
    }

    private static CallSnapshot toDomain(CallContextEntity source) {
        CallSnapshot target = new CallSnapshot();
        target.setPersistenceId(source.getId()); target.setSourceId(source.getSourceCallId()); target.setPosition(source.getPosition());
        target.setQueuePosition(source.getQueuePosition()); target.setDirection(source.getDirection()); target.setCounterparty(source.getCounterparty());
        target.setStatus(source.getStatus()); target.setKnownFacts(new ArrayList<>(source.getKnownFacts()));
        target.setHiddenFacts(new ArrayList<>(source.getHiddenFacts())); target.setAiContext(source.getAiContext()); target.setGender(source.getGender());
        target.setEmotionalState(source.getEmotionalState()); target.setApplicant(toDomain(source.getApplicant()));
        return target;
    }

    private static CallContextEntity toEntity(CallSnapshot source) {
        CallContextEntity target = new CallContextEntity();
        target.setId(source.getPersistenceId()); target.setSourceCallId(source.getSourceId()); target.setPosition(source.getPosition());
        target.setQueuePosition(source.getQueuePosition()); target.setDirection(source.getDirection()); target.setCounterparty(source.getCounterparty());
        target.setStatus(source.getStatus()); target.setKnownFacts(new ArrayList<>(source.getKnownFacts()));
        target.setHiddenFacts(new ArrayList<>(source.getHiddenFacts())); target.setAiContext(source.getAiContext()); target.setGender(source.getGender());
        target.setEmotionalState(source.getEmotionalState()); target.setApplicant(toEntity(source.getApplicant()));
        return target;
    }

    static SolutionCardRevision toDomain(SolutionContextEntity source) {
        SolutionCardRevision target = new SolutionCardRevision();
        target.setId(source.getId()); target.setContextId(source.getContext().getUuid()); target.setCardId(source.getCardId());
        target.setPreviousRevisionId(source.getPreviousRevisionId()); target.setVersion(source.getVersion());
        target.setCallId(source.getCallId()); target.setMainCardId(source.getMainCardId());
        target.setApplicant(toDomain(source.getApplicant())); target.setVictim(toDomain(source.getVictim()));
        target.setAdditionalInfo(new HashMap<>(source.getAdditionalInfo())); target.setAdditionalInfoProvided(source.isAdditionalInfoProvided());
        target.setIncidentType(source.getIncidentType()); target.setCreatedAt(source.getCreatedAt());
        return target;
    }

    static SolutionContextEntity toEntity(SolutionCardRevision source) {
        SolutionContextEntity target = new SolutionContextEntity();
        target.setId(source.getId()); target.setCardId(source.getCardId()); target.setPreviousRevisionId(source.getPreviousRevisionId());
        target.setVersion(source.getVersion()); target.setCallId(source.getCallId());
        target.setMainCardId(source.getMainCardId());
        target.setApplicant(toEntity(source.getApplicant())); target.setVictim(toEntity(source.getVictim()));
        target.setAdditionalInfo(new HashMap<>(source.getAdditionalInfo())); target.setAdditionalInfoProvided(source.isAdditionalInfoProvided());
        target.setIncidentType(source.getIncidentType());
        return target;
    }

    private static com.simulator112.contextmanager.domain.common.Address toDomain(com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Address value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.common.Address(
                value.getCity(), value.getStreet(), value.getHouse(), value.getBuilding(), value.getApartment(), value.getFloor());
    }
    private static com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Address toEntity(com.simulator112.contextmanager.domain.common.Address value) {
        return value == null ? null : new com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Address(
                value.city(), value.street(), value.house(), value.building(), value.apartment(), value.floor());
    }
    private static com.simulator112.contextmanager.domain.common.Person toDomain(Applicant value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.common.Person(value.getFirstName(), value.getLastName(),
                value.getMiddleName(), value.getAge(), value.getPhone(), value.getContactPhone(), value.getAddress(), value.getAdditionalInfo(), value.getEmotionalState());
    }
    private static Applicant toEntity(com.simulator112.contextmanager.domain.common.Person value) {
        return value == null ? null : new Applicant(value.firstName(), value.lastName(), value.middleName(), value.age(), value.phone(),
                value.contactPhone(), value.address(), value.additionalInfo(), value.emotionalState());
    }
    private static com.simulator112.contextmanager.domain.common.Criteria toDomain(DispatcherCriteria value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.common.Criteria(
                value.getRequiredQuestions(), value.getExpectedActions(), value.getCriticalMistakes());
    }
    private static DispatcherCriteria toEntity(com.simulator112.contextmanager.domain.common.Criteria value) {
        return value == null ? null : new DispatcherCriteria(new ArrayList<>(value.requiredQuestions()),
                new ArrayList<>(value.expectedActions()), new ArrayList<>(value.criticalMistakes()));
    }
    private static com.simulator112.contextmanager.domain.system112.PersonInfo toDomain(com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.PersonInfo value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.system112.PersonInfo(value.getPhone(), value.getContactPhone(),
                value.getLastName(), value.getFirstName(), value.getMiddleName(), value.getAddress(), value.getAdditionalInfo());
    }
    private static com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.PersonInfo toEntity(com.simulator112.contextmanager.domain.system112.PersonInfo value) {
        return value == null ? null : new com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.PersonInfo(value.phone(), value.contactPhone(),
                value.lastName(), value.firstName(), value.middleName(), value.address(), value.additionalInfo());
    }
}
