package com.simulator112.adminservice.dto;

import java.util.List;

public record MonitoringOverviewResponse(
    List<ServiceStatus> services, Double dbPoolActive, Double dbPoolPending, Double diskAvailPercent) {}
