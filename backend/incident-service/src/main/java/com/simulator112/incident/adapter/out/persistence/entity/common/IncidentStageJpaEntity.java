package com.simulator112.incident.adapter.out.persistence.entity.common;

import com.simulator112.incident.adapter.out.persistence.entity.dds.DdsStageDetailsJpaEntity;
import com.simulator112.incident.adapter.out.persistence.entity.system112.System112StageDetailsJpaEntity;
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
@Table(name = "incident_stages")
public class IncidentStageJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentJpaEntity incident;

    private Integer position;
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    private System112StageDetailsJpaEntity system112Details;

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    private DdsStageDetailsJpaEntity ddsDetails;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CallScenarioJpaEntity> calls = new ArrayList<>();

    public void setSystem112Details(System112StageDetailsJpaEntity details) {
        system112Details = details;
        if (details != null) {
            details.setStage(this);
        }
    }

    public void setDdsDetails(DdsStageDetailsJpaEntity details) {
        ddsDetails = details;
        if (details != null) {
            details.setStage(this);
        }
    }

    public void addCall(CallScenarioJpaEntity call) {
        calls.add(call);
        call.setStage(this);
    }
}
