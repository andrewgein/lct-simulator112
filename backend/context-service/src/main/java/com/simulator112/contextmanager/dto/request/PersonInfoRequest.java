package com.simulator112.contextmanager.dto.request;

public record PersonInfoRequest(
        String phone,
        String contactPhone,
        String lastName,
        String firstName,
        String middleName,
        String address,
        String additionalInfo) {
}
