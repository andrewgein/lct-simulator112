package com.simulator112.course.adapter.out.persistence.entity;

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
@Table(name = "course_enrollments")
public class EnrollmentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "materials_completed_at")
    private Instant materialsCompletedAt;

    @ElementCollection
    @CollectionTable(name = "course_enrollment_completed_assignments",
            joinColumns = @JoinColumn(name = "enrollment_id"))
    @OrderColumn(name = "position")
    @Column(name = "assignment_id", nullable = false)
    private List<UUID> completedAssignmentIds = new ArrayList<>();

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
