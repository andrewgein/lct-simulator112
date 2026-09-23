package com.simulator112.course.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "course_materials")
public class CourseMaterialJpaEntity {
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

    @Column(name = "file_object_key", nullable = false, length = 512)
    private String fileObjectKey;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_content_type")
    private String fileContentType;

    @Column(name = "file_size")
    private Long fileSize;
}
