package com.simulator112.contextmanager.adapter.out.persistence.entity.system112;

import com.simulator112.contextmanager.adapter.out.persistence.entity.common.StageContextEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system112_stage_contexts")
@Getter
@Setter
public class System112StageContextEntity {
    @Id
    private UUID stageContextId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_context_id")
    private StageContextEntity stage;

    @ElementCollection
    @CollectionTable(name = "system112_stage_classifier_codes", joinColumns = @JoinColumn(name = "stage_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "classifier_code", nullable = false, length = 50)
    private List<String> classifierCodes = new ArrayList<>();

    @Column(name = "victim_count", nullable = false)
    private int victimCount;
}
