package com.simulator112.incident.adapter.out.persistence.entity.system112;

import com.simulator112.incident.adapter.out.persistence.entity.common.IncidentStageJpaEntity;
import com.simulator112.incident.adapter.out.persistence.entity.common.PersonEmbeddable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system112_stage_details")
public class System112StageDetailsJpaEntity {
    @Id
    @Column(name = "stage_id")
    private UUID stageId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id")
    private IncidentStageJpaEntity stage;

    @Column(name = "classifier_code", nullable = false, length = 50)
    private String classifierCode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "victim_first_name")),
            @AttributeOverride(name = "lastName", column = @Column(name = "victim_last_name")),
            @AttributeOverride(name = "middleName", column = @Column(name = "victim_middle_name")),
            @AttributeOverride(name = "age", column = @Column(name = "victim_age")),
            @AttributeOverride(name = "phone", column = @Column(name = "victim_phone")),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "victim_contact_phone")),
            @AttributeOverride(name = "address", column = @Column(name = "victim_address")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "victim_additional_info"))
    })
    private PersonEmbeddable victim;
}
