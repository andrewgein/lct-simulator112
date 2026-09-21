package com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DispatcherCriteria {

    @ElementCollection
    @CollectionTable(name = "incident_context_required_questions", joinColumns = @JoinColumn(name = "incident_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "question", nullable = false)
    private List<String> requiredQuestions;

    @ElementCollection
    @CollectionTable(name = "incident_context_expected_actions", joinColumns = @JoinColumn(name = "incident_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "action", nullable = false)
    private List<String> expectedActions;

    @ElementCollection
    @CollectionTable(name = "incident_context_critical_mistakes", joinColumns = @JoinColumn(name = "incident_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "mistake", nullable = false)
    private List<String> criticalMistakes;
}
