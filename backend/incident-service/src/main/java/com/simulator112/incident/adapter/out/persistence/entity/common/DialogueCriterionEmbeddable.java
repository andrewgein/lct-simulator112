package com.simulator112.incident.adapter.out.persistence.entity.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DialogueCriterionEmbeddable {
    @Column(name = "criterion_id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "hypothesis", nullable = false, columnDefinition = "text")
    private String hypothesis;

    @Column(name = "weight", nullable = false)
    private int weight;
}
