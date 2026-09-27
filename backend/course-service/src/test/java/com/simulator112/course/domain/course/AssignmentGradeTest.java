package com.simulator112.course.domain.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentGradeTest {
    private Assignment assignment(Integer forThree, Integer forFour, Integer forFive) {
        return new Assignment(null, "Задание", null, AssignmentDifficulty.NORMAL,
                AssignmentExecutionMode.SEQUENTIAL, List.of(), forThree, forFour, forFive);
    }

    @Test
    void storesAscendingPercentageThresholds() {
        Assignment assignment = assignment(40, 60, 80);
        assertThat(assignment.threshold3()).isEqualTo(40);
        assertThat(assignment.threshold4()).isEqualTo(60);
        assertThat(assignment.threshold5()).isEqualTo(80);
    }

    @Test
    void supportsLegacyAssignmentsWithoutThresholds() {
        assertThat(assignment(null, null, null).threshold3()).isNull();
    }

    @Test
    void rejectsPartialOrUnorderedThresholds() {
        assertThatThrownBy(() -> assignment(40, null, 80)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assignment(60, 60, 80)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assignment(40, 70, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assignment(-1, 60, 80)).isInstanceOf(IllegalArgumentException.class);
    }
}
