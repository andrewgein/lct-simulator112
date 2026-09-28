package com.simulator112.contextmanager.adapter.out.persistence.entity.common;

import com.simulator112.contextmanager.domain.common.Gender;
import com.simulator112.contextmanager.domain.common.CallDirection;
import com.simulator112.contextmanager.domain.common.CounterpartyType;
import com.simulator112.contextmanager.domain.common.CallStatus;
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
@Table(name = "call_contexts")
@Getter
@Setter
public class CallContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_call_id", nullable = false, updatable = false)
    private UUID sourceCallId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_context_id", nullable = false)
    private StageContextEntity stage;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounterpartyType counterparty;

    @Column(name = "service_code", length = 100)
    private String serviceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;

    @ElementCollection
    @CollectionTable(name = "call_context_known_facts", joinColumns = @JoinColumn(name = "call_context_id"))
    @OrderColumn(name = "position")
    @Column(name = "fact", nullable = false)
    private List<String> knownFacts;

    @ElementCollection
    @CollectionTable(name = "call_context_hidden_facts", joinColumns = @JoinColumn(name = "call_context_id"))
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
            @AttributeOverride(name = "onScenePhone", column = @Column(name = "applicant_on_scene_phone")),
            @AttributeOverride(name = "address", column = @Column(name = "applicant_address")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "applicant_additional_info", columnDefinition = "text")),
            @AttributeOverride(name = "emotionalState", column = @Column(name = "applicant_emotional_state"))
    })
    private Applicant applicant;

}
