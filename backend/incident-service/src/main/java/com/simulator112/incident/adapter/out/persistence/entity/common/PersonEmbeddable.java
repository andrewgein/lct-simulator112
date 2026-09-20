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
public class PersonEmbeddable {
    private String firstName;
    private String lastName;
    private String middleName;
    private Integer age;
    private String phone;
    private String contactPhone;
    private String address;
    private String additionalInfo;
}
