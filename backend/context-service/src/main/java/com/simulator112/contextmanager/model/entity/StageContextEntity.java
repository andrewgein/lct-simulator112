package com.simulator112.contextmanager.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simulator112.contextmanager.model.embeddable.Applicant;
import com.simulator112.contextmanager.model.embeddable.IncidentTypeInfo;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
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

    @Column(nullable = false)
    private Integer position;

    private String title;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "type_ref_id")),
            @AttributeOverride(name = "typeId", column = @Column(name = "type_business_id")),
            @AttributeOverride(name = "serviceType", column = @Column(name = "type_service_type")),
            @AttributeOverride(name = "typeName", column = @Column(name = "type_name"))
    })
    private IncidentTypeInfo type;

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
    private List<StageAdditionalInfoContextEntity> additionalInfo = new ArrayList<>();

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<DialupContextEntity> dialups = new ArrayList<>();

    public void addAdditionalInfo(StageAdditionalInfoContextEntity value) {
        additionalInfo.add(value);
        value.setStage(this);
    }

    public void addDialup(DialupContextEntity dialup) {
        dialups.add(dialup);
        dialup.setStage(this);
    }
}