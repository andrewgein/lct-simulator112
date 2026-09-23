package com.simulator112.course.adapter.out.persistence.entity;

import com.simulator112.course.domain.course.CourseTargetType;
import com.simulator112.course.domain.course.DdsService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "courses")
public class CourseJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private CourseTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dds_service")
    private DdsService ddsService;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<CourseMaterialJpaEntity> materials = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AssignmentJpaEntity> assignments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void addMaterial(CourseMaterialJpaEntity material) {
        material.setPosition(materials.size());
        materials.add(material);
        material.setCourse(this);
    }

    public void addAssignment(AssignmentJpaEntity assignment) {
        assignment.setPosition(assignments.size());
        assignments.add(assignment);
        assignment.setCourse(this);
    }
}
