package com.simulator112.contextmanager.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.DialogProgressStatus;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import com.simulator112.shared.dto.Difficulty;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "contexts")
@Getter
@Setter
public class Context {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    private UUID levelId;

    private String levelTitle;

    @Enumerated(EnumType.STRING)
    private IncidentTargetType targetType;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    private ExecutionMode executionMode;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private ContextStatus status;

    private UUID activeCallId;

    @Enumerated(EnumType.STRING)
    private DialogProgressStatus dialogStatus;

    @OneToMany(mappedBy = "context", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IncidentContextEntity> incidentContexts = new ArrayList<>();

    @OneToMany(mappedBy = "context", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SolutionContextEntity> solutionContexts = new ArrayList<>();

    @OneToOne(mappedBy = "context", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DialogContextEntity dialogContext;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void attachIncidentContext(IncidentContextEntity incidentContext) {
        incidentContext.setContext(this);
        this.incidentContexts.add(incidentContext);
    }

    public void attachSolutionContext(SolutionContextEntity solutionContext) {
        solutionContext.setContext(this);
        this.solutionContexts.add(solutionContext);
    }

    public void attachDialogContext(DialogContextEntity dialogContext) {
        dialogContext.setContext(this);
        this.dialogContext = dialogContext;
    }
}
