package com.simulator112.contextmanager.adapter.out.persistence.entity.dds;

import com.simulator112.contextmanager.domain.common.ReactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionStatusEventSnapshot {
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_status", nullable = false, length = 40)
    private ReactionStatus status;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;
}
