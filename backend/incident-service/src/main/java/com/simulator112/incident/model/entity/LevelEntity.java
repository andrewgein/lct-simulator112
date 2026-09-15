package com.simulator112.incident.model.entity;

import com.simulator112.incident.model.enums.Difficulty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
@Table(name = "levels")
public class LevelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @OneToMany(mappedBy = "level")
    @Builder.Default
    private List<IncidentEntity> incidents = new ArrayList<>();

    public void addIncident(IncidentEntity incident) {
        incidents.add(incident);
        incident.setLevel(this);
    }

    public void removeIncident(IncidentEntity incident) {
        incidents.remove(incident);
        incident.setLevel(null);
    }
}
