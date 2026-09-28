package com.simulator112.incident.application.service.generation;

import com.simulator112.incident.application.service.generation.dds.steps.DdsBasicFieldsStep;
import com.simulator112.incident.application.service.generation.dds.steps.DdsIntermediateStageStep;
import com.simulator112.incident.application.service.generation.dds.steps.DdsPreparedCardStep;
import com.simulator112.incident.application.service.generation.system112.steps.System112CriteriaStep;
import com.simulator112.incident.application.service.generation.system112.steps.System112StageStep;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepPathsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void basicAndCardStepsOwnDifferentRoots() {
        var draft = mapper.createObjectNode();
        assertThat(DdsBasicFieldsStep.PATHS.matches("/address/city")).isTrue();
        DdsBasicFieldsStep.PATHS.check("/address/city", draft);
        assertThat(DdsBasicFieldsStep.PATHS.matches("/preparedCardTemplate/applicant/phone")).isFalse();
        assertThatThrownBy(() -> DdsBasicFieldsStep.PATHS.check("/address/id", draft))
                .isInstanceOf(IllegalStateException.class);
        assertThat(DdsPreparedCardStep.PATHS.matches("/preparedCardTemplate/applicant/phone")).isTrue();
        DdsPreparedCardStep.PATHS.check("/preparedCardTemplate/applicant/phone", draft);
        assertThatThrownBy(() -> DdsPreparedCardStep.PATHS.check("/preparedCardTemplate/assignedServices", draft))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stageStepsKeepTheirOwnFieldsAndDdsBoundaryRule() {
        var draft = mapper.readTree("""
                {"stages":[{"calls":[]},{"calls":[{"person":{}}]},{"calls":[]}]}
                """);
        assertThatThrownBy(() -> DdsIntermediateStageStep.PATHS.check("/stages/0/title", draft))
                .isInstanceOf(IllegalStateException.class);
        DdsIntermediateStageStep.PATHS.check("/stages/1/calls/0/serviceCode", draft);
        assertThatThrownBy(() -> System112StageStep.PATHS.check("/stages/1/calls/0/serviceCode", draft))
                .isInstanceOf(IllegalStateException.class);
        System112StageStep.PATHS.check("/stages/0/title", draft);
        assertThatThrownBy(() -> System112StageStep.PATHS.check("/stages/1/calls/0/id", draft))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void criteriaStepChecksExistingIndicesAndEditableFields() {
        var draft = mapper.readTree("""
                {"dialogueCriteria":[{"weight":10}]}
                """);
        System112CriteriaStep.PATHS.check("/dialogueCriteria/0/weight", draft);
        assertThatThrownBy(() -> System112CriteriaStep.PATHS.check("/dialogueCriteria/1/weight", draft))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> System112CriteriaStep.PATHS.check("/dialogueCriteria/0/id", draft))
                .isInstanceOf(IllegalStateException.class);
    }
}
