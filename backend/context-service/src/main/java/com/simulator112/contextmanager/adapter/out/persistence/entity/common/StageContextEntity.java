package com.simulator112.contextmanager.adapter.out.persistence.entity.common;

import com.simulator112.contextmanager.adapter.out.persistence.entity.dds.DdsStageContextEntity;
import com.simulator112.contextmanager.adapter.out.persistence.entity.system112.System112StageContextEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simulator112.contextmanager.domain.common.StageStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;import jakarta.persistence.OrderBy;
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

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    private System112StageContextEntity system112;

    @OneToOne(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    private DdsStageContextEntity dds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StageStatus status;

    private java.time.Instant startedAt;

    private java.time.Instant deadlineAt;

    @Column(columnDefinition = "text")
    private String description;


    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CallContextEntity> calls = new ArrayList<>();

    public void setSystem112(System112StageContextEntity details) {
        system112 = details;
        if (details != null) details.setStage(this);
    }

    public void setDds(DdsStageContextEntity details) {
        dds = details;
        if (details != null) details.setStage(this);
    }

    public void addCall(CallContextEntity call) {
        calls.add(call);
        call.setStage(this);
    }
}
