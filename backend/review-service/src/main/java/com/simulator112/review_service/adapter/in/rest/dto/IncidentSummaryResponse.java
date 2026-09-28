package com.simulator112.review_service.adapter.in.rest.dto;

import java.util.List;

public record IncidentSummaryResponse(String id, int order, String title, int victimCount,
                                      List<String> classifierCodes) {
}
