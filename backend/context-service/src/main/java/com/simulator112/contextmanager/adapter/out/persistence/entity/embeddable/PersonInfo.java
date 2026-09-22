package com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable;

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
    private String onScenePhone;
    private String lastName;
    private String firstName;
    private String middleName;
    private String status;
    private String address;
    private String additionalInfo;
}
