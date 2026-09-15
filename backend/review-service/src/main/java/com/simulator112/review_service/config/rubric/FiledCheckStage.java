package com.simulator112.review_service.config.rubric;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.context.grpc.contract.PersonInfo;
import com.simulator112.incident.grpc.contract.Applicant;
import com.simulator112.incident.grpc.contract.DialupContext;
import com.simulator112.incident.grpc.contract.IncidentAdditionalInfo;
import com.simulator112.incident.grpc.contract.StageContext;
import com.simulator112.review_service.model.entity.CriterionResult;
import com.simulator112.review_service.service.Criterion;
import com.simulator112.review_service.service.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FiledCheckStage extends Stage {
    public FiledCheckStage() {
        super("Проверка заполненных полей", List.of(
                new ApplicantFieldsComplaint(),
                new VictimFieldsComplaint(),
                new AdditionalFieldsComplaint()));
    }
}

final class ScenarioState {
    private ScenarioState() {
    }

    static StageContext latestStage(FullContext context) {
        return context.getIncidentContext().getStagesList().stream()
                .max(Comparator.comparingInt(StageContext::getPosition))
                .orElse(StageContext.getDefaultInstance());
    }
}

class ApplicantFieldsComplaint extends Criterion {
    public ApplicantFieldsComplaint() {
        super("Проверка полей заявителя", 20);
    }

    @Override
    public List<CriterionResult> evaluate(FullContext context) {
        Applicant correctApplicant = ScenarioState.latestStage(context).getDialupsList().stream()
                .max(Comparator.comparingInt(DialupContext::getPosition))
                .map(DialupContext::getApplicant)
                .orElse(Applicant.getDefaultInstance());
        PersonInfo filledApplicant = context.getSolutionContext().getApplicant();

        return List.of(
                resultOf(FieldValueComparator.hasSameContent(correctApplicant.getFirstName(), filledApplicant.getFirstName()),
                        "Имя заявителя указано верно", "Имя заявителя указано неверно", 5),

                resultOf(FieldValueComparator.hasSameContent(correctApplicant.getLastName(),filledApplicant.getLastName()),
                        "Фамилия заявителя указано верно", "Фамилия заявителя указано неверно", 5),

                resultOf(FieldValueComparator.hasSameContent(correctApplicant.getMiddleName(),filledApplicant.getMiddleName()),
                        "Отчество заявителя указано верно", "Отчество заявителя указано неверно", 5),

                resultOf(FieldValueComparator.hasSameContent(correctApplicant.getPhone(),filledApplicant.getPhone()),
                        "Телефон заявителя указан верно", "Телефон заявителя указан неверно", 5));

    }
}

class VictimFieldsComplaint extends Criterion {
    public VictimFieldsComplaint() {
        super("Проверка полей пострадавшего", 20);
    }

    @Override
    public List<CriterionResult> evaluate(FullContext context) {
        Applicant correctVictim = ScenarioState.latestStage(context).getVictim();
        PersonInfo filledVictim = context.getSolutionContext().getVictim();

        return List.of(
                resultOf(FieldValueComparator.hasSameContent(correctVictim.getFirstName(),filledVictim.getFirstName()),
                        "Имя пострадавшего указано верно", "Имя пострадавшего указано неверно", 5),

                resultOf(FieldValueComparator.hasSameContent(correctVictim.getLastName(),filledVictim.getLastName()),
                        "Фамилия пострадавшего указано верно", "Фамилия пострадавшего указано неверно", 5),

                resultOf(FieldValueComparator.hasSameContent(correctVictim.getMiddleName(),filledVictim.getMiddleName()),
                        "Отчество пострадавшего указано верно", "Отчество пострадавшего указано неверно", 5),

                resultOf(FieldValueComparator.hasSameContent(correctVictim.getPhone(),filledVictim.getPhone()),
                        "Телефон пострадавшего указан верно", "Телефон пострадавшего указан неверно", 5));
    }
}

class AdditionalFieldsComplaint extends Criterion {
    public AdditionalFieldsComplaint() {
        super("Проверка дополнительных полей", 10);
    }

    @Override
    public List<CriterionResult> evaluate(FullContext context) {
        List<IncidentAdditionalInfo> correctFields = ScenarioState.latestStage(context).getAdditionalInfoList();
        Map<String, String> filledFields = context.getSolutionContext().getAdditionalInfoMap();

        if (correctFields.isEmpty()) return List.of();
        int scorePerField = getMaxScore() / correctFields.size();

        return correctFields.stream()
                .map(field -> resultOf(
                        FieldValueComparator.hasSameContent(field.getFieldValue(), filledFields.get(field.getAdditionalInfoId())),
                        "Поле \"" + field.getFieldName() + "\" заполнено верно",
                        "Поле \"" + field.getFieldName() + "\" заполнено неверно",
                        scorePerField))
                .collect(Collectors.toList());
    }
}
