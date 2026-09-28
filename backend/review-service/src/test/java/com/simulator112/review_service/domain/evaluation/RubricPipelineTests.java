package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RubricPipelineTests {
    @Test
    void system112RubricExposesStageAndCriteria() {
        var rubric = new System112ReviewRubric();

        assertThat(rubric.stages()).hasSize(1);
        assertThat(rubric.stages().getFirst().criteria()).extracting(Criterion::name)
                .containsExactly("Поля", "Операции и связи", "Обработка звонков");
        assertThat(rubric.stages().getFirst().maxScore()).isEqualTo(100);
    }

    @Test
    void ddsRubricExposesStageAndCriteria() {
        var rubric = new DdsReviewRubric();

        assertThat(rubric.stages()).hasSize(1);
        assertThat(rubric.stages().getFirst().criteria()).extracting(Criterion::name)
                .containsExactly("Своевременность статусов и звонки");
        assertThat(rubric.stages().getFirst().maxScore()).isEqualTo(100);
    }

    @Test
    void builderRejectsRubricWhoseCriteriaDoNotTotalOneHundred() {
        Criterion criterion = new Criterion("Неполный критерий", 50) {
            @Override
            public List<CriterionResult> evaluate(ReviewSubmission submission) {
                return List.of();
            }
        };
        var builder = new RubricBuilder(ReviewSubmission.TargetType.SYSTEM_112)
                .addStage(new Stage("Этап", List.of(criterion)));

        assertThrows(IllegalStateException.class, builder::build);
    }
}
