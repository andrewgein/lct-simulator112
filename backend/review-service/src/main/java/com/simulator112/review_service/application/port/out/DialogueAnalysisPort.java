package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.ReviewSubmission;

import java.util.List;

public interface DialogueAnalysisPort {
    List<DialogueAnalysis> analyze(List<ReviewSubmission.TranscriptPhrase> transcript,
                                   List<ReviewSubmission.DialogueCriterion> criteria);

    record DialogueAnalysis(String criterionId, boolean matched, double confidence) {
    }
}
