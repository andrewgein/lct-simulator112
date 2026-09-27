package com.simulator112.adminservice.application.port.in;

import java.util.List;

public interface GetMonitoringOverviewUseCase {
  Overview get();

  record ServiceHealth(String service, boolean up, Double http5xxRate, Double heapUsedBytes) {}

  record Overview(
      List<ServiceHealth> services, Double dbPoolActive, Double dbPoolPending, Double diskAvailPercent) {}
}
