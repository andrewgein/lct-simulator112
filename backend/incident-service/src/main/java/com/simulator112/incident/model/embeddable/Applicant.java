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
public class Applicant {

    private String firstName;
    private String lastName;
    private String middleName;
    private Integer age;
    private String phone;
    private String contactPhone;
    private String address;
    private String additionalInfo;
}
