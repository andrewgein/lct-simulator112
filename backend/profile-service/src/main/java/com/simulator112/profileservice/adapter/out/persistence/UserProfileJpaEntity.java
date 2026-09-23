package com.simulator112.profileservice.adapter.out.persistence;

import com.simulator112.profileservice.domain.model.DdsService;
import com.simulator112.profileservice.domain.model.TrainingTrack;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_profiles")
class UserProfileJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    private String patronymic;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_track")
    private TrainingTrack trainingTrack;

    @Enumerated(EnumType.STRING)
    @Column(name = "dds_service")
    private DdsService ddsService;

    @Version
    @Column(nullable = false)
    private long version;

    @UpdateTimestamp
    private Instant updatedAt;
}
