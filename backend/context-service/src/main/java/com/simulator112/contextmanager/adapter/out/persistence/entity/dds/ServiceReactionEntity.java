package com.simulator112.contextmanager.adapter.out.persistence.entity.dds;

import com.simulator112.contextmanager.adapter.out.persistence.entity.common.IncidentContextEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "context_service_reactions")
@Getter
@Setter
public class ServiceReactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_context_id", nullable = false)
    private IncidentContextEntity incidentContext;

    @Column(name = "service_code", nullable = false, length = 100)
    private String serviceCode;

    @ElementCollection
    @CollectionTable(name = "context_service_reaction_history",
            joinColumns = @JoinColumn(name = "service_reaction_id"))
    @OrderColumn(name = "position")
    private List<ReactionStatusEventSnapshot> history = new ArrayList<>();
}
