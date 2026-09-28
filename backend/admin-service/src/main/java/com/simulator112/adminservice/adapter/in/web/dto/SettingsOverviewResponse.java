package com.simulator112.adminservice.adapter.in.web.dto;

import java.util.Map;

public record SettingsOverviewResponse(
    Map<String, Object> gateway, Map<String, Object> auth, Map<String, Object> dialog) {}
