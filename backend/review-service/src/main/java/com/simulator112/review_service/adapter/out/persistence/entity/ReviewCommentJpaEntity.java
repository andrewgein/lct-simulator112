package com.simulator112.review_service.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_comments")
@Getter
@Setter
public class ReviewCommentJpaEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_context_id", nullable = false)
    private ReviewJpaEntity review;
    @Column(nullable = false)
    private UUID authorId;
    @Column(name = "comment_text", nullable = false, columnDefinition = "text")
    private String text;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
