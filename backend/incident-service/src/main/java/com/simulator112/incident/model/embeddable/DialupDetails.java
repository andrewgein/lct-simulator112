package com.simulator112.incident.model.embeddable;

import com.simulator112.incident.model.enums.Gender;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class DialupDetails {

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dialup_known_facts", joinColumns = @JoinColumn(name = "dialup_id"))
    @OrderColumn(name = "position")
    @Column(name = "fact", nullable = false)
    private List<String> knownFacts;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dialup_hidden_facts", joinColumns = @JoinColumn(name = "dialup_id"))
    @OrderColumn(name = "position")
    @Column(name = "fact", nullable = false)
    private List<String> hiddenFacts;

    @Column(columnDefinition = "text")
    private String aiContext;

    private String emotionalState;
}
