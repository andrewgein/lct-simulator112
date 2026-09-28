package com.simulator112.contextmanager.adapter.out.persistence.entity.system112;

import jakarta.persistence.CollectionTable;
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
    @CollectionTable(name = "incident_context_dialogue_criteria", joinColumns = @JoinColumn(name = "incident_context_id"))
    @OrderColumn(name = "position")
    private List<DialogueCriterionEmbeddable> dialogueCriteria;
}
