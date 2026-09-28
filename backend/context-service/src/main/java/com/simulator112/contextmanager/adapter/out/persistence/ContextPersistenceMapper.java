package com.simulator112.contextmanager.adapter.out.persistence;

import com.simulator112.contextmanager.adapter.out.persistence.entity.common.Applicant;
import com.simulator112.contextmanager.adapter.out.persistence.entity.common.CallContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.common.Context;
import com.simulator112.contextmanager.adapter.out.persistence.entity.common.DialogContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.common.IncidentContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.common.StageContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.dds.DdsStageContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.dds.ReactionStatusEventSnapshot;
import com.simulator112.contextmanager.adapter.out.persistence.entity.dds.ServiceReactionEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.system112.DialogueCriterionEmbeddable;
import com.simulator112.contextmanager.adapter.out.persistence.entity.system112.DispatcherCriteria;
import com.simulator112.contextmanager.adapter.out.persistence.entity.system112.SolutionContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.system112.System112StageContextEntity;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.DialogueCriterion;
import com.simulator112.contextmanager.domain.common.DialogTranscript;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.contextmanager.domain.common.Phrase;
import com.simulator112.contextmanager.domain.common.ReactionStatusEvent;
import com.simulator112.contextmanager.domain.common.ServiceReaction;
import com.simulator112.contextmanager.domain.common.StageSnapshot;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.dds.DdsStageDetails;
import com.simulator112.contextmanager.domain.system112.System112StageDetails;
import com.simulator112.contextmanager.domain.system112.SolutionCardRevision;
import java.util.ArrayList;
import java.util.HashMap;

final class ContextPersistenceMapper {
    private ContextPersistenceMapper() {}

    static TrainingContext toDomain(Context source) {
        TrainingContext target = new TrainingContext();
        target.setId(source.getUuid()); target.setAssignmentId(source.getAssignmentId());
        target.setLevelTitle(source.getLevelTitle());
        target.setThreshold3(source.getThreshold3()); target.setThreshold4(source.getThreshold4()); target.setThreshold5(source.getThreshold5());
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
        target.setUuid(source.getId()); target.setAssignmentId(source.getAssignmentId());
        target.setLevelTitle(source.getLevelTitle());
        target.setThreshold3(source.getThreshold3()); target.setThreshold4(source.getThreshold4()); target.setThreshold5(source.getThreshold5());
        target.setTargetType(source.getTargetType()); target.setDifficulty(source.getDifficulty());
        target.setExecutionMode(source.getExecutionMode()); target.setUserId(source.getUserId());
        target.setStatus(source.getStatus()); target.setActiveCallId(source.getActiveCallId()); target.setDialogStatus(source.getDialogStatus());
        source.getIncidents().stream().map(ContextPersistenceMapper::toEntity).forEach(target::attachIncidentContext);
        source.getSolutionCards().stream().map(ContextPersistenceMapper::toEntity).forEach(target::attachSolutionContext);
        if (source.getDialog() != null) {
            DialogContextEntity dialog = new DialogContextEntity();
            dialog.setContextId(source.getId());
            dialog.setTranscript(source.getDialog().phrases().stream()
                    .map(value -> new com.simulator112.contextmanager.adapter.out.persistence.entity.common.Phrase(value.speaker(), value.text()))
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
        target.setAddress(toDomain(source.getAddress()));
        target.setCriteria(toDomain(source.getDispatcherCriteria())); target.setPreparedCardClassifierCodes(new ArrayList<>(source.getPreparedCardClassifierCodes()));
        target.setCardApplicant(toDomain(source.getCardApplicant())); target.setCardVictimCount(source.getCardVictimCount());
        target.setPreparedCardAdditionalInfo(new java.util.LinkedHashMap<>(source.getPreparedCardAdditionalInfo()));
        target.setInitialAssignmentService(source.getInitialAssignmentService());
        target.setServiceReactions(source.getServiceReactions().stream().map(ContextPersistenceMapper::toDomain)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        target.setStages(source.getStages().stream().map(ContextPersistenceMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return target;
    }

    private static IncidentContextEntity toEntity(IncidentSnapshot source) {
        IncidentContextEntity target = new IncidentContextEntity();
        target.setId(source.getPersistenceId()); target.setSourceIncidentId(source.getSourceId()); target.setPosition(source.getPosition());
        target.setTitle(source.getTitle()); target.setStatus(source.getStatus()); target.setActiveStageId(source.getActiveStageId());
        target.setAddress(toEntity(source.getAddress()));
        target.setDispatcherCriteria(toEntity(source.getCriteria())); target.setPreparedCardClassifierCodes(new ArrayList<>(source.getPreparedCardClassifierCodes()));
        target.setCardApplicant(toEntity(source.getCardApplicant())); target.setCardVictimCount(source.getCardVictimCount());
        target.setPreparedCardAdditionalInfo(new java.util.LinkedHashMap<>(source.getPreparedCardAdditionalInfo()));
        target.setInitialAssignmentService(source.getInitialAssignmentService());
        source.getServiceReactions().stream().map(ContextPersistenceMapper::toEntity).forEach(target::addServiceReaction);
        source.getStages().stream().map(ContextPersistenceMapper::toEntity).forEach(target::addStage);
        return target;
    }

    private static ServiceReaction toDomain(ServiceReactionEntity source) {
        return new ServiceReaction(source.getId(), source.getServiceCode(), source.getHistory().stream()
                .map(value -> new ReactionStatusEvent(value.getStatus(), value.getChangedAt(), value.getComment())).toList());
    }

    private static ServiceReactionEntity toEntity(ServiceReaction source) {
        ServiceReactionEntity target = new ServiceReactionEntity();
        target.setId(source.getPersistenceId());
        target.setServiceCode(source.getServiceCode());
        target.setHistory(source.getHistory().stream().map(value -> new ReactionStatusEventSnapshot(
                value.status(), value.changedAt(), value.comment())).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return target;
    }

    private static StageSnapshot toDomain(StageContextEntity source) {
        if ((source.getSystem112() == null) == (source.getDds() == null)) {
            throw new IllegalStateException("У этапа должна быть ровно одна специализация: " + source.getSourceStageId());
        }
        StageSnapshot target = new StageSnapshot();
        target.setPersistenceId(source.getId()); target.setSourceId(source.getSourceStageId()); target.setPosition(source.getPosition());
        target.setTitle(source.getTitle()); target.setStatus(source.getStatus()); target.setStartedAt(source.getStartedAt());
        target.setDeadlineAt(source.getDeadlineAt()); target.setDescription(source.getDescription());
        if (source.getSystem112() != null) {
            target.setSystem112(new System112StageDetails(source.getSystem112().getClassifierCodes(),
                    source.getSystem112().getVictimCount()));
        }
        if (source.getDds() != null) {
            var details = source.getDds();
            target.setDds(new DdsStageDetails(details.getType(), details.getTimeLimitSeconds(),
                    details.getExpectedComment(), details.getComment(), details.getActualStatus()));
        }
        target.setCalls(source.getCalls().stream().map(ContextPersistenceMapper::toDomain).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        return target;
    }

    private static StageContextEntity toEntity(StageSnapshot source) {
        if ((source.getSystem112() == null) == (source.getDds() == null)) {
            throw new IllegalStateException("У этапа должна быть ровно одна специализация: " + source.getSourceId());
        }
        StageContextEntity target = new StageContextEntity();
        target.setId(source.getPersistenceId()); target.setSourceStageId(source.getSourceId()); target.setPosition(source.getPosition());
        target.setTitle(source.getTitle()); target.setStatus(source.getStatus()); target.setStartedAt(source.getStartedAt());
        target.setDeadlineAt(source.getDeadlineAt()); target.setDescription(source.getDescription());
        if (source.getSystem112() != null) {
            var details = new System112StageContextEntity();
            details.setStageContextId(source.getPersistenceId());
            details.setClassifierCodes(new ArrayList<>(source.getSystem112().classifierCodes()));
            details.setVictimCount(source.getSystem112().victimCount());
            target.setSystem112(details);
        }
        if (source.getDds() != null) {
            var details = new DdsStageContextEntity();
            details.setStageContextId(source.getPersistenceId());
            details.setType(source.getDds().getType()); details.setTimeLimitSeconds(source.getDds().getTimeLimitSeconds());
            details.setExpectedComment(source.getDds().getExpectedComment()); details.setComment(source.getDds().getComment());
            details.setActualStatus(source.getDds().getActualStatus());
            target.setDds(details);
        }
        source.getCalls().stream().map(ContextPersistenceMapper::toEntity).forEach(target::addCall);
        return target;
    }

    private static CallSnapshot toDomain(CallContextEntity source) {
        CallSnapshot target = new CallSnapshot();
        target.setPersistenceId(source.getId()); target.setSourceId(source.getSourceCallId()); target.setPosition(source.getPosition());
        target.setQueuePosition(source.getQueuePosition()); target.setDirection(source.getDirection()); target.setCounterparty(source.getCounterparty());
        target.setServiceCode(source.getServiceCode()); target.setStatus(source.getStatus()); target.setKnownFacts(new ArrayList<>(source.getKnownFacts()));
        target.setHiddenFacts(new ArrayList<>(source.getHiddenFacts())); target.setAiContext(source.getAiContext()); target.setGender(source.getGender());
        target.setEmotionalState(source.getEmotionalState()); target.setApplicant(toDomain(source.getApplicant()));
        return target;
    }

    private static CallContextEntity toEntity(CallSnapshot source) {
        CallContextEntity target = new CallContextEntity();
        target.setId(source.getPersistenceId()); target.setSourceCallId(source.getSourceId()); target.setPosition(source.getPosition());
        target.setQueuePosition(source.getQueuePosition()); target.setDirection(source.getDirection()); target.setCounterparty(source.getCounterparty());
        target.setServiceCode(source.getServiceCode()); target.setStatus(source.getStatus()); target.setKnownFacts(new ArrayList<>(source.getKnownFacts()));
        target.setHiddenFacts(new ArrayList<>(source.getHiddenFacts())); target.setAiContext(source.getAiContext()); target.setGender(source.getGender());
        target.setEmotionalState(source.getEmotionalState()); target.setApplicant(toEntity(source.getApplicant()));
        return target;
    }

    static SolutionCardRevision toDomain(SolutionContextEntity source) {
        SolutionCardRevision target = new SolutionCardRevision();
        target.setId(source.getId()); target.setContextId(source.getContext().getUuid()); target.setCardId(source.getCardId());
        target.setPreviousRevisionId(source.getPreviousRevisionId()); target.setVersion(source.getVersion());
        target.setCallId(source.getCallId()); target.setMainCardId(source.getMainCardId());
        target.setApplicant(toDomain(source.getApplicant())); target.setVictimCount(source.getVictimCount());
        target.setAdditionalInfo(new HashMap<>(source.getAdditionalInfo())); target.setAdditionalInfoProvided(source.isAdditionalInfoProvided());
        target.setIncidentTypes(new ArrayList<>(source.getIncidentTypes())); target.setServices(new ArrayList<>(source.getServices()));
        target.setCreatedAt(source.getCreatedAt());
        return target;
    }

    static SolutionContextEntity toEntity(SolutionCardRevision source) {
        SolutionContextEntity target = new SolutionContextEntity();
        target.setId(source.getId()); target.setCardId(source.getCardId()); target.setPreviousRevisionId(source.getPreviousRevisionId());
        target.setVersion(source.getVersion()); target.setCallId(source.getCallId());
        target.setMainCardId(source.getMainCardId());
        target.setApplicant(toEntity(source.getApplicant())); target.setVictimCount(source.getVictimCount());
        target.setAdditionalInfo(new HashMap<>(source.getAdditionalInfo())); target.setAdditionalInfoProvided(source.isAdditionalInfoProvided());
        target.setIncidentTypes(new ArrayList<>(source.getIncidentTypes())); target.setServices(new ArrayList<>(source.getServices()));
        return target;
    }

    private static com.simulator112.contextmanager.domain.common.Address toDomain(com.simulator112.contextmanager.adapter.out.persistence.entity.common.Address value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.common.Address(
                value.getCity(), value.getStreet(), value.getHouse(), value.getBuilding(), value.getApartment(), value.getFloor());
    }
    private static com.simulator112.contextmanager.adapter.out.persistence.entity.common.Address toEntity(com.simulator112.contextmanager.domain.common.Address value) {
        return value == null ? null : new com.simulator112.contextmanager.adapter.out.persistence.entity.common.Address(
                value.city(), value.street(), value.house(), value.building(), value.apartment(), value.floor());
    }
    private static com.simulator112.contextmanager.domain.common.Person toDomain(Applicant value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.common.Person(value.getFirstName(), value.getLastName(),
                value.getMiddleName(), value.getAge(), value.getPhone(), value.getContactPhone(), value.getOnScenePhone(),
                value.getAddress(), value.getAdditionalInfo(), value.getEmotionalState());
    }
    private static Applicant toEntity(com.simulator112.contextmanager.domain.common.Person value) {
        return value == null ? null : new Applicant(value.firstName(), value.lastName(), value.middleName(), value.age(), value.phone(),
                value.contactPhone(), value.onScenePhone(), value.address(), value.additionalInfo(), value.emotionalState());
    }
    private static com.simulator112.contextmanager.domain.common.Criteria toDomain(DispatcherCriteria value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.common.Criteria(
                value.getDialogueCriteria().stream().map(criterion -> new DialogueCriterion(
                        criterion.getId(), criterion.getName(), criterion.getHypothesis(), criterion.getWeight())).toList());
    }
    private static DispatcherCriteria toEntity(com.simulator112.contextmanager.domain.common.Criteria value) {
        return value == null ? null : new DispatcherCriteria(value.dialogueCriteria().stream()
                .map(criterion -> new DialogueCriterionEmbeddable(
                        criterion.id(), criterion.name(), criterion.hypothesis(), criterion.weight()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
    }
    private static com.simulator112.contextmanager.domain.system112.PersonInfo toDomain(com.simulator112.contextmanager.adapter.out.persistence.entity.system112.PersonInfo value) {
        return value == null ? null : new com.simulator112.contextmanager.domain.system112.PersonInfo(value.getPhone(), value.getContactPhone(),
                value.getOnScenePhone(), value.getLastName(), value.getFirstName(), value.getMiddleName(), value.getStatus(),
                value.getAddress(), value.getAdditionalInfo());
    }
    private static com.simulator112.contextmanager.adapter.out.persistence.entity.system112.PersonInfo toEntity(com.simulator112.contextmanager.domain.system112.PersonInfo value) {
        return value == null ? null : new com.simulator112.contextmanager.adapter.out.persistence.entity.system112.PersonInfo(value.phone(), value.contactPhone(),
                value.onScenePhone(), value.lastName(), value.firstName(), value.middleName(), value.status(), value.address(),
                value.additionalInfo());
    }
}
