package com.simulator112.review_service.adapter.in.rest.dto;

import java.util.List;
import java.util.Map;

public record DispatcherCardResponse(String cardId, String callId, String mainCardId, String incidentId, PersonResponse applicant,
                                     Integer victimCount, List<String> incidentTypes, List<String> services,
                                     Map<String, String> additionalInfo) {
    public record PersonResponse(String firstName, String lastName, String middleName, String phone,
                                 String contactPhone, String onScenePhone, String address, String additionalInfo) {
    }
}
