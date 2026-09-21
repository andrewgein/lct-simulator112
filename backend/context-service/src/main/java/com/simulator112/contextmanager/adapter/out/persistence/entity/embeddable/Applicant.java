package com.simulator112.contextmanager.adapter.out.persistence.entity.embeddable;

import com.simulator112.shared.dto.EmotionalState;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    private EmotionalState emotionalState;
}
