package com.simulator112.contextmanager.model.embeddable;

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
public class PersonInfo {

    private String phone;
    private String contactPhone;
    private String lastName;
    private String firstName;
    private String middleName;
    private String address;
    private String additionalInfo;
}
