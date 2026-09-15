package com.simulator112.review_service.service;

import java.util.LinkedList;
import java.util.List;

public class RubricBuilder {
    private final List<Stage> stages = new LinkedList<>();
    public RubricBuilder addStage(Stage stage) {
        this.stages.add(stage);
        return this;
    }

    public Rubric build() {
        int totalScore = stages.stream().mapToInt(Stage::getMaxScore).sum();
        /*if (totalScore < 100) {
            throw new IllegalStateException("Total score of rubric is < 100");
        }*/
        return new Rubric(stages);
    }
}
