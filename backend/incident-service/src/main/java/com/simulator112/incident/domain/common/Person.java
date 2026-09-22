package com.simulator112.incident.domain.common;

public record Person(
        String firstName,
        String lastName,
        String middleName,
        Integer age,
        String phone,
        String contactPhone,
        String onScenePhone,
        String address,
        String additionalInfo) {
}
