package com.simulator112.contextmanager.adapter.out.persistence.entity;

import com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable.PersonInfo;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "solution_contexts")
@Getter
@Setter
public class SolutionContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "context_id", nullable = false)
    private Context context;

    @Column(name = "card_id", nullable = false, updatable = false)
    private UUID cardId;

    @Column(name = "previous_revision_id", updatable = false)
    private UUID previousRevisionId;

    @Column(nullable = false, updatable = false)
    private long version;

    @Column(name = "call_id", nullable = false, updatable = false)
    private UUID callId;

    @Column(name = "main_card_id", updatable = false)
    private UUID mainCardId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "phone", column = @Column(name = "applicant_phone", updatable = false)),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "applicant_contact_phone", updatable = false)),
            @AttributeOverride(name = "lastName", column = @Column(name = "applicant_last_name", updatable = false)),
            @AttributeOverride(name = "firstName", column = @Column(name = "applicant_first_name", updatable = false)),
            @AttributeOverride(name = "middleName", column = @Column(name = "applicant_middle_name", updatable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "applicant_address", updatable = false)),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "applicant_additional_info", columnDefinition = "text", updatable = false))
    })
    private PersonInfo applicant;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "phone", column = @Column(name = "victim_phone", updatable = false)),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "victim_contact_phone", updatable = false)),
            @AttributeOverride(name = "lastName", column = @Column(name = "victim_last_name", updatable = false)),
            @AttributeOverride(name = "firstName", column = @Column(name = "victim_first_name", updatable = false)),
            @AttributeOverride(name = "middleName", column = @Column(name = "victim_middle_name", updatable = false)),
            @AttributeOverride(name = "address", column = @Column(name = "victim_address", updatable = false)),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "victim_additional_info", columnDefinition = "text", updatable = false))
    })
    private PersonInfo victim;

    @ElementCollection
    @CollectionTable(name = "solution_context_additional_info", joinColumns = @JoinColumn(name = "solution_context_id"))
    @MapKeyColumn(name = "info_key")
    @Column(name = "info_value", columnDefinition = "text")
    private Map<String, String> additionalInfo = new HashMap<>();

    @Column(name = "additional_info_provided", nullable = false, updatable = false)
    private boolean additionalInfoProvided;

    @Column(name = "incident_type", updatable = false)
    private String incidentType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
