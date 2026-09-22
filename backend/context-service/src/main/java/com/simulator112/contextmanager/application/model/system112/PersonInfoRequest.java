package com.simulator112.contextmanager.application.model.system112;

public record PersonInfoRequest(
        String phone,
        String contactPhone,
        String onScenePhone,
        String lastName,
        String firstName,
        String middleName,
        String address,
        String additionalInfo) {
}
