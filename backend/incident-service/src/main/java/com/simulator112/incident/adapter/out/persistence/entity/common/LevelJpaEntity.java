package com.simulator112.incident.adapter.out.persistence.entity.common;

import com.simulator112.incident.domain.common.Difficulty;
import com.simulator112.incident.domain.common.IncidentTargetType;
import com.simulator112.incident.domain.level.ExecutionMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "levels")
public class LevelJpaEntity {
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

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false)
    private ExecutionMode executionMode;

    @ManyToMany
    @JoinTable(
            name = "level_incidents",
            joinColumns = @JoinColumn(name = "level_id"),
            inverseJoinColumns = @JoinColumn(name = "incident_id"))
    @OrderColumn(name = "position")
    private List<IncidentJpaEntity> incidents = new ArrayList<>();
}
