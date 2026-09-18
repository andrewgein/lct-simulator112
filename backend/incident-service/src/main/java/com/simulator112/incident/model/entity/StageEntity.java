package com.simulator112.incident.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simulator112.incident.model.embeddable.Applicant;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stages")
public class StageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentEntity incident;

    @Column(name = "position", nullable = false)
    private Integer position;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classifier_entry_id", nullable = false)
    private ClassifierEntryEntity classifierEntry;

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
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "victim_additional_info", columnDefinition = "text"))
    })
    private Applicant victim;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<DialupEntity> dialups = new ArrayList<>();

    public void addDialup(DialupEntity dialup) {
        dialups.add(dialup);
        dialup.setStage(this);
    }

    public void removeDialup(DialupEntity dialup) {
        dialups.remove(dialup);
        dialup.setStage(null);
    }
}
