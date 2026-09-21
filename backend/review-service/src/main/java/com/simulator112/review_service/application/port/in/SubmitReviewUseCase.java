package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewSubmission;

public interface SubmitReviewUseCase {
    Review submit(ReviewSubmission submission);
}
