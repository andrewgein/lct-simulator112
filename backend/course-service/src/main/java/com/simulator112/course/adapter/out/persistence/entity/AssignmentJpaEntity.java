package com.simulator112.course.adapter.out.persistence.entity;

import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.AssignmentExecutionMode;
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
@Table(name = "course_assignments")
public class AssignmentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseJpaEntity course;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false)
    private AssignmentExecutionMode executionMode;

    @ElementCollection
    @CollectionTable(name = "course_assignment_incidents", joinColumns = @JoinColumn(name = "assignment_id"))
    @OrderColumn(name = "position")
    @Column(name = "incident_id", nullable = false)
    private List<UUID> incidentIds = new ArrayList<>();
}
