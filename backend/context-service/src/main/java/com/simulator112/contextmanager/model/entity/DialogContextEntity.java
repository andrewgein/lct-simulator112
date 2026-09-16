package com.simulator112.contextmanager.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simulator112.contextmanager.model.embeddable.Phrase;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "dialog_contexts")
@Getter
@Setter
public class DialogContextEntity {

    @Id
    private UUID contextId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "context_id")
    private Context context;

    @ElementCollection
    @CollectionTable(name = "dialog_phrases", joinColumns = @JoinColumn(name = "context_id"))
    @OrderColumn(name = "position")
    private List<Phrase> transcript = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
