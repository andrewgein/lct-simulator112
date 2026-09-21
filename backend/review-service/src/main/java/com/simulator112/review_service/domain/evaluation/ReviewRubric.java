package com.simulator112.review_service.domain.evaluation;

import com.simulator112.review_service.domain.model.CriterionResult;
import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.List;

public interface ReviewRubric {
    boolean supports(ReviewSubmission submission);

    List<CriterionResult> evaluate(ReviewSubmission submission);
}
