package com.simulator112.incident.model.entity;

import com.simulator112.incident.model.embeddable.Applicant;
import com.simulator112.incident.model.embeddable.DialupDetails;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dialups")
public class DialupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private StageEntity stage;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Embedded
    private DialupDetails dialupDetails;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "applicant_first_name")),
            @AttributeOverride(name = "lastName", column = @Column(name = "applicant_last_name")),
            @AttributeOverride(name = "middleName", column = @Column(name = "applicant_middle_name")),
            @AttributeOverride(name = "age", column = @Column(name = "applicant_age")),
            @AttributeOverride(name = "phone", column = @Column(name = "applicant_phone")),
            @AttributeOverride(name = "contactPhone", column = @Column(name = "applicant_contact_phone")),
            @AttributeOverride(name = "address", column = @Column(name = "applicant_address")),
            @AttributeOverride(name = "additionalInfo", column = @Column(name = "applicant_additional_info", columnDefinition = "text"))
    })
    private Applicant applicant;

}
