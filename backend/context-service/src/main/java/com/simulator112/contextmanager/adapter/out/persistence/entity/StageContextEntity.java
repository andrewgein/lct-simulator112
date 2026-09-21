package com.simulator112.contextmanager.adapter.out.persistence.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.Applicant;
import com.simulator112.contextmanager.domain.dds.DdsStageType;
import com.simulator112.contextmanager.domain.common.StageStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stage_contexts")
@Getter
@Setter
public class StageContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_stage_id", nullable = false, updatable = false)
    private UUID sourceStageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_context_id", nullable = false)
    private IncidentContextEntity incidentContext;

    private Integer position;

    private String title;

    @ElementCollection
    @CollectionTable(name = "stage_context_classifier_codes", joinColumns = @JoinColumn(name = "stage_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "classifier_code", nullable = false, length = 50)
    private List<String> classifierCodes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private DdsStageType ddsStageType;

    private Integer timeLimitSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageStatus status;

    private java.time.Instant startedAt;

    private java.time.Instant deadlineAt;

    @Column(columnDefinition = "text")
    private String description;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "victim_first_name")),
            @AttributeOverride(name = "lastName", column = @Column(name = "victim_last_name")),
            @AttributeOverride(name = "middleName", column = @Column(name = "victim_middle_name")),
            @AttributeOverride(name = "age", column = @Column(name = "victim_age")),
            @AttributeOverride(name = "phone", column = @Column(name = "victim_phone")),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "victim_contact_phone")),
            @AttributeOverride(name = "address", column = @Column(name = "victim_address")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "victim_additional_info", columnDefinition = "text")),
            @AttributeOverride(name = "emotionalState", column = @Column(name = "victim_emotional_state"))
    })
    private Applicant victim;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CallContextEntity> calls = new ArrayList<>();

    public void addCall(CallContextEntity call) {
        calls.add(call);
        call.setStage(this);
    }
}