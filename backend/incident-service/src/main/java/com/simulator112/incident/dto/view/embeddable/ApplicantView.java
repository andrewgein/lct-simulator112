package com.simulator112.incident.dto.view.embeddable;

public record ApplicantView(
        String firstName,
        String lastName,
        String middleName,
        Integer age,
        String phone,
        String contactPhone,
        String address,
        String additionalInfo
) {
}
