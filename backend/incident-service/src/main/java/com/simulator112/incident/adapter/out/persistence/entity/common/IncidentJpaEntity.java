package com.simulator112.incident.adapter.out.persistence.entity.common;

import com.simulator112.incident.adapter.out.persistence.entity.dds.DdsStageTransitionEmbeddable;
import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "incidents")
public class IncidentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private IncidentTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "city", column = @Column(name = "address_city")),
            @AttributeOverride(name = "street", column = @Column(name = "address_street")),
            @AttributeOverride(name = "house", column = @Column(name = "address_house")),
            @AttributeOverride(name = "building", column = @Column(name = "address_building")),
            @AttributeOverride(name = "apartment", column = @Column(name = "address_apartment")),
            @AttributeOverride(name = "floor", column = @Column(name = "address_floor"))
    })
    private AddressEmbeddable address;

    @Column(name = "emergency_service")
    private String emergencyService;

    @Column(name = "dds_initial_stage_id")
    private UUID ddsInitialStageId;

    @ElementCollection
    @CollectionTable(name = "prepared_card_classifier_codes", joinColumns = @JoinColumn(name = "incident_id"))
    @OrderColumn(name = "position")
    @Column(name = "classifier_code", nullable = false, length = 50)
    private List<String> preparedCardClassifierCodes = new ArrayList<>();

    @Column(name = "initial_assignment_classifier_code")
    private String initialAssignmentClassifierCode;

    @Column(name = "initial_assignment_instructions", columnDefinition = "text")
    private String initialAssignmentInstructions;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "card_applicant_first_name")),
            @AttributeOverride(name = "lastName", column = @Column(name = "card_applicant_last_name")),
            @AttributeOverride(name = "middleName", column = @Column(name = "card_applicant_middle_name")),
            @AttributeOverride(name = "age", column = @Column(name = "card_applicant_age")),
            @AttributeOverride(name = "phone", column = @Column(name = "card_applicant_phone")),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "card_applicant_contact_phone")),
            @AttributeOverride(name = "onScenePhone", column = @Column(name = "card_applicant_on_scene_phone")),
            @AttributeOverride(name = "address", column = @Column(name = "card_applicant_address")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "card_applicant_additional_info"))
    })
    private PersonEmbeddable cardApplicant;

    @Column(name = "card_victim_count", nullable = false)
    private int cardVictimCount;

    @ElementCollection
    @CollectionTable(name = "prepared_card_additional_info", joinColumns = @JoinColumn(name = "incident_id"))
    @MapKeyColumn(name = "info_key")
    @Column(name = "info_value")
    private Map<String, String> preparedCardAdditionalInfo = new LinkedHashMap<>();

    @ElementCollection
    @CollectionTable(name = "dds_stage_transitions", joinColumns = @JoinColumn(name = "incident_id"))
    private List<DdsStageTransitionEmbeddable> ddsStageTransitions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "incident_dialogue_criteria", joinColumns = @JoinColumn(name = "incident_id"))
    @OrderColumn(name = "position")
    private List<DialogueCriterionEmbeddable> dialogueCriteria = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<IncidentStageJpaEntity> stages = new ArrayList<>();

    public void addStage(IncidentStageJpaEntity stage) {
        stages.add(stage);
        stage.setIncident(this);
    }
}
