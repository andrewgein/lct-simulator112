package com.simulator112.incident.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simulator112.incident.model.enums.ServiceType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "types")
public class TypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "service_type")
    private ServiceType serviceType;

    @Column(nullable = false, name = "type_id")
    private String typeId;

    @Column(nullable = false, name = "type_name")
    private String typeName;

    @OneToMany(mappedBy = "type", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<TypeInstructionEntity> instructions = new ArrayList<>();

    @OneToMany(mappedBy = "type", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<AdditionalInfoEntity> fields = new ArrayList<>();

    public void addInstruction(TypeInstructionEntity instruction) {
        instructions.add(instruction);
        instruction.setType(this);
    }

    public void addField(AdditionalInfoEntity field) {
        fields.add(field);
        field.setType(this);
    }
}
