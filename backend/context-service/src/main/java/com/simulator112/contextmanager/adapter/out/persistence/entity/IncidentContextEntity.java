package com.simulator112.contextmanager.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Address;
import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Applicant;
import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.DispatcherCriteria;
import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.DdsStageTransitionSnapshot;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "incident_contexts")
@Getter
@Setter
public class IncidentContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "context_id", nullable = false)
    private Context context;

    @Column(name = "source_incident_id", nullable = false, updatable = false)
    private UUID sourceIncidentId;

    @Column(nullable = false)
    private int position;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentProgressStatus status;

    private UUID activeStageId;

    private UUID initialStageId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "city", column = @Column(name = "address_city")),
            @AttributeOverride(name = "street", column = @Column(name = "address_street")),
            @AttributeOverride(name = "house", column = @Column(name = "address_house")),
            @AttributeOverride(name = "building", column = @Column(name = "address_building")),
            @AttributeOverride(name = "apartment", column = @Column(name = "address_apartment")),
            @AttributeOverride(name = "floor", column = @Column(name = "address_floor"))
    })
    private Address address;

    @Embedded
    private DispatcherCriteria dispatcherCriteria;

    @ElementCollection
    @CollectionTable(name = "context_prepared_card_classifier_codes",
            joinColumns = @JoinColumn(name = "incident_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "classifier_code", nullable = false, length = 50)
    private List<String> preparedCardClassifierCodes = new ArrayList<>();

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
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "card_applicant_additional_info")),
            @AttributeOverride(name = "emotionalState", column = @Column(name = "card_applicant_emotional_state"))
    })
    private Applicant cardApplicant;

    @Column(name = "card_victim_count", nullable = false)
    private int cardVictimCount;

    @ElementCollection
    @CollectionTable(name = "context_prepared_card_additional_info",
            joinColumns = @JoinColumn(name = "incident_context_id"))
    @MapKeyColumn(name = "info_key")
    @Column(name = "info_value")
    private Map<String, String> preparedCardAdditionalInfo = new LinkedHashMap<>();

    private String initialAssignmentService;
    private String initialAssignmentClassifierCode;

    @Column(columnDefinition = "text")
    private String initialAssignmentInstructions;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "context_dds_stage_transitions",
            joinColumns = @JoinColumn(name = "incident_context_id"))
    @OrderColumn(name = "position")
    private List<DdsStageTransitionSnapshot> transitions = new ArrayList<>();

    @OneToMany(mappedBy = "incidentContext", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<StageContextEntity> stages = new ArrayList<>();

    public void addStage(StageContextEntity stage) {
        stages.add(stage);
        stage.setIncidentContext(this);
    }

}
