package com.simulator112.adminservice.adapter.in.web;

import com.simulator112.adminservice.adapter.in.web.dto.MonitoringOverviewResponse;
import com.simulator112.adminservice.adapter.in.web.dto.ServiceStatus;
import com.simulator112.adminservice.application.port.in.GetMonitoringOverviewUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitoringController {

  private final GetMonitoringOverviewUseCase getMonitoringOverview;

  public MonitoringController(GetMonitoringOverviewUseCase getMonitoringOverview) {
    this.getMonitoringOverview = getMonitoringOverview;
  }

  @GetMapping("/api/v1/admin/monitoring/overview")
  public MonitoringOverviewResponse overview() {
    GetMonitoringOverviewUseCase.Overview overview = getMonitoringOverview.get();
    return new MonitoringOverviewResponse(
        overview.services().stream()
            .map(service -> new ServiceStatus(
                service.service(), service.up(), service.http5xxRate(), service.heapUsedBytes()))
            .toList(),
        overview.dbPoolActive(),
        overview.dbPoolPending(),
        overview.diskAvailPercent());
  }
}
