package com.simulator112.adminservice.application.service;

import com.simulator112.adminservice.application.port.in.GetMonitoringOverviewUseCase;
import com.simulator112.adminservice.application.port.out.MetricsQueryPort;
import com.simulator112.adminservice.domain.model.PrometheusSample;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonitoringApplicationService implements GetMonitoringOverviewUseCase {

  private final MetricsQueryPort metrics;

  @Override
  public Overview get() {
    Map<String, Boolean> up = new HashMap<>();
    for (PrometheusSample sample : metrics.query("up{job=\"sim112-stable\"}")) {
      up.put(serviceName(sample.label("instance")), sample.value() == 1.0);
    }
    for (PrometheusSample sample : metrics.query("up{job=\"dialog-service\"}")) {
      up.put(serviceName(sample.label("instance")), sample.value() == 1.0);
    }

    Map<String, Double> errorRate = new HashMap<>();
    for (PrometheusSample sample :
        metrics.query(
            "sum by (instance) (rate(http_server_requests_seconds_count{outcome=\"SERVER_ERROR\"}[5m]))")) {
      errorRate.put(serviceName(sample.label("instance")), sample.value());
    }

    Map<String, Double> heapUsed = new HashMap<>();
    for (PrometheusSample sample : metrics.query("jvm_memory_used_bytes{area=\"heap\"}")) {
      heapUsed.merge(serviceName(sample.label("instance")), sample.value(), Double::sum);
    }

    List<ServiceHealth> services =
        up.entrySet().stream()
            .map(
                entry ->
                    new ServiceHealth(
                        entry.getKey(),
                        entry.getValue(),
                        errorRate.get(entry.getKey()),
                        heapUsed.get(entry.getKey())))
            .sorted((a, b) -> a.service().compareTo(b.service()))
            .toList();

    Double poolActive = firstValue(metrics.query("sum(hikaricp_connections_active)"));
    Double poolPending = firstValue(metrics.query("sum(hikaricp_connections_pending)"));

    Double diskAvail = firstValue(metrics.query("min(node_filesystem_avail_bytes{mountpoint=\"/\"})"));
    Double diskTotal = firstValue(metrics.query("min(node_filesystem_size_bytes{mountpoint=\"/\"})"));
    Double diskAvailPercent =
        (diskAvail != null && diskTotal != null && diskTotal > 0) ? (diskAvail / diskTotal) * 100 : null;

    return new Overview(services, poolActive, poolPending, diskAvailPercent);
  }

  private static Double firstValue(List<PrometheusSample> samples) {
    return samples.isEmpty() ? null : samples.get(0).value();
  }

  private static String serviceName(String instance) {
    if (instance == null) return "unknown";
    String withoutPort = instance.contains(":") ? instance.substring(0, instance.indexOf(':')) : instance;
    int lastDash = withoutPort.lastIndexOf('-');
    return lastDash >= 0 ? withoutPort.substring(lastDash + 1) : withoutPort;
  }
}
