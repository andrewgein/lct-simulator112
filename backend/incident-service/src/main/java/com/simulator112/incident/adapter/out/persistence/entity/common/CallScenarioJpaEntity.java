package com.simulator112.incident.adapter.out.persistence.entity.common;

import com.simulator112.incident.domain.common.CallDirection;
import com.simulator112.incident.domain.common.CounterpartyType;
import com.simulator112.incident.domain.common.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "call_scenarios")
public class CallScenarioJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private IncidentStageJpaEntity stage;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounterpartyType counterparty;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "ai_context", columnDefinition = "text")
    private String aiContext;

    @Column(name = "emotional_state")
    private String emotionalState;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "person_first_name")),
            @AttributeOverride(name = "lastName", column = @Column(name = "person_last_name")),
            @AttributeOverride(name = "middleName", column = @Column(name = "person_middle_name")),
            @AttributeOverride(name = "age", column = @Column(name = "person_age")),
            @AttributeOverride(name = "phone", column = @Column(name = "person_phone")),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "person_contact_phone")),
            @AttributeOverride(name = "onScenePhone", column = @Column(name = "person_on_scene_phone")),
            @AttributeOverride(name = "address", column = @Column(name = "person_address")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "person_additional_info"))
    })
    private PersonEmbeddable person;

    @ElementCollection
    @CollectionTable(name = "call_scenario_known_facts", joinColumns = @JoinColumn(name = "call_id"))
    @OrderColumn(name = "position")
    @Column(name = "fact", nullable = false)
    private List<String> knownFacts = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "call_scenario_hidden_facts", joinColumns = @JoinColumn(name = "call_id"))
    @OrderColumn(name = "position")
    @Column(name = "fact", nullable = false)
    private List<String> hiddenFacts = new ArrayList<>();
}
