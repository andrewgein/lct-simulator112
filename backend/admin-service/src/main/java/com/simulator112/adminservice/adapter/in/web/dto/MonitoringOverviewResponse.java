package com.simulator112.adminservice.adapter.in.web.dto;

import java.util.List;

public record MonitoringOverviewResponse(
    List<ServiceStatus> services, Double dbPoolActive, Double dbPoolPending, Double diskAvailPercent) {}
