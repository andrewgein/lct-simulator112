package com.simulator112.incident.model.embeddable;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_required_questions", joinColumns = @JoinColumn(name = "incident_id"))
    @OrderColumn(name = "position")
    @Column(name = "question", nullable = false)
    private List<String> requiredQuestions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_expected_actions", joinColumns = @JoinColumn(name = "incident_id"))
    @OrderColumn(name = "position")
    @Column(name = "action", nullable = false)
    private List<String> expectedActions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_critical_mistakes", joinColumns = @JoinColumn(name = "incident_id"))
    @OrderColumn(name = "position")
    @Column(name = "mistake", nullable = false)
    private List<String> criticalMistakes;
}
