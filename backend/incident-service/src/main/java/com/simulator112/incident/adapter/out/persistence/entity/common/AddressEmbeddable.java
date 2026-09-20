package com.simulator112.incident.adapter.out.persistence.entity.common;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AddressEmbeddable {
    private String city;
    private String street;
    private String house;
    private String building;
    private String apartment;
    private Integer floor;
}
