package com.simulator112.contextmanager.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "stage_context_additional_info")
@Getter
@Setter
public class StageAdditionalInfoContextEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_context_id", nullable = false)
    private StageContextEntity stage;

    private String sourceAdditionalInfoId;
    private String fieldCode;
    private String fieldName;
    private String fieldType;
    private boolean required;
    private String fieldValue;
}
