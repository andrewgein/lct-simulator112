package com.simulator112.contextmanager.model.entity;

import com.simulator112.contextmanager.model.embeddable.Applicant;
import com.simulator112.contextmanager.model.enums.Gender;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dialup_contexts")
@Getter
@Setter
public class DialupContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_dialup_id", nullable = false, updatable = false)
    private UUID sourceDialupId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_context_id", nullable = false)
    private StageContextEntity stage;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition;

    @ElementCollection
    @CollectionTable(name = "dialup_context_known_facts", joinColumns = @JoinColumn(name = "dialup_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "fact", nullable = false)
    private List<String> knownFacts;

    @ElementCollection
    @CollectionTable(name = "dialup_context_hidden_facts", joinColumns = @JoinColumn(name = "dialup_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "fact", nullable = false)
    private List<String> hiddenFacts;

    @Column(columnDefinition = "text")
    private String aiContext;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String emotionalState;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "applicant_first_name")),
            @AttributeOverride(name = "lastName", column = @Column(name = "applicant_last_name")),
            @AttributeOverride(name = "middleName", column = @Column(name = "applicant_middle_name")),
            @AttributeOverride(name = "age", column = @Column(name = "applicant_age")),
            @AttributeOverride(name = "phone", column = @Column(name = "applicant_phone")),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "applicant_contact_phone")),
            @AttributeOverride(name = "address", column = @Column(name = "applicant_address")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "applicant_additional_info", columnDefinition = "text")),
            @AttributeOverride(name = "emotionalState", column = @Column(name = "applicant_emotional_state"))
    })
    private Applicant applicant;

}
