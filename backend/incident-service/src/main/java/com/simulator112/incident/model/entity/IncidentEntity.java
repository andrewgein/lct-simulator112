package com.simulator112.incident.model.entity;

import com.simulator112.incident.model.embeddable.Address;
import com.simulator112.incident.model.embeddable.DispatcherCriteria;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidents")
public class IncidentEntity {
        
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private LevelEntity level;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "city", column = @Column(name = "address_city")),
            @AttributeOverride(name = "street", column = @Column(name = "address_street")),
            @AttributeOverride(name = "house", column = @Column(name = "address_house")),
            @AttributeOverride(name = "building", column = @Column(name = "address_building")),
            @AttributeOverride(name = "apartment", column = @Column(name = "address_apartment")),
            @AttributeOverride(name = "floor", column = @Column(name = "address_floor"))
    })
    private Address address;

    @Embedded
    private DispatcherCriteria criteria;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<StageEntity> stages = new ArrayList<>();

    public void addStage(StageEntity stage) {
        stages.add(stage);
        stage.setIncident(this);
    }

    public void removeStage(StageEntity stage) {
        stages.remove(stage);
        stage.setIncident(null);
    }
}
