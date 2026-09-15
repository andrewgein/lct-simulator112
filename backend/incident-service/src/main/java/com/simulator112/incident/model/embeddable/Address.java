package com.simulator112.incident.model.embeddable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {

    private String city;

    private String street;

    private String house;

    private String building;

    private String apartment;

    private Integer floor;
}
