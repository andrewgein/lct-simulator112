package com.simulator112.classifier.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dispatch_services")
public class DispatchServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "dispatchService", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<RoutingVariantEntity> routingVariants = new ArrayList<>();

    public void addRoutingVariant(RoutingVariantEntity routingVariant) {
        routingVariants.add(routingVariant);
        routingVariant.setDispatchService(this);
    }
}
