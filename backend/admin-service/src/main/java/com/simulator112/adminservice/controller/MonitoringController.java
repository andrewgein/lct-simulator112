package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.MonitoringOverviewResponse;
import com.simulator112.adminservice.dto.ServiceStatus;
import com.simulator112.adminservice.service.PrometheusClient;
import com.simulator112.adminservice.service.PrometheusClient.PrometheusSample;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitoringController {

  private final PrometheusClient prometheus;

  public MonitoringController(PrometheusClient prometheus) {
    this.prometheus = prometheus;
  }

  @GetMapping("/api/v1/admin/monitoring/overview")
  public MonitoringOverviewResponse overview() {
    Map<String, Boolean> up = new HashMap<>();
    for (PrometheusSample sample : prometheus.query("up{job=\"sim112-stable\"}")) {
      up.put(serviceName(sample.label("instance")), sample.value() == 1.0);
    }
    for (PrometheusSample sample : prometheus.query("up{job=\"dialog-service\"}")) {
      up.put(serviceName(sample.label("instance")), sample.value() == 1.0);
    }

    Map<String, Double> errorRate = new HashMap<>();
    for (PrometheusSample sample :
        prometheus.query(
            "sum by (instance) (rate(http_server_requests_seconds_count{outcome=\"SERVER_ERROR\"}[5m]))")) {
      errorRate.put(serviceName(sample.label("instance")), sample.value());
    }

    Map<String, Double> heapUsed = new HashMap<>();
    for (PrometheusSample sample :
        prometheus.query("jvm_memory_used_bytes{area=\"heap\"}")) {
      heapUsed.merge(serviceName(sample.label("instance")), sample.value(), Double::sum);
    }

    List<ServiceStatus> services =
        up.entrySet().stream()
            .map(
                entry ->
                    new ServiceStatus(
                        entry.getKey(),
                        entry.getValue(),
                        errorRate.get(entry.getKey()),
                        heapUsed.get(entry.getKey())))
            .sorted((a, b) -> a.service().compareTo(b.service()))
            .toList();

    Double poolActive = firstValue(prometheus.query("sum(hikaricp_connections_active)"));
    Double poolPending = firstValue(prometheus.query("sum(hikaricp_connections_pending)"));

    Double diskAvail = firstValue(prometheus.query("min(node_filesystem_avail_bytes{mountpoint=\"/\"})"));
    Double diskTotal = firstValue(prometheus.query("min(node_filesystem_size_bytes{mountpoint=\"/\"})"));
    Double diskAvailPercent =
        (diskAvail != null && diskTotal != null && diskTotal > 0) ? (diskAvail / diskTotal) * 100 : null;

    return new MonitoringOverviewResponse(services, poolActive, poolPending, diskAvailPercent);
  }

  private static Double firstValue(List<PrometheusSample> samples) {
    return samples.isEmpty() ? null : samples.get(0).value();
  }

  /** "sim112-stable-auth:8081" -> "auth"; falls back to the raw instance label if it doesn't
   * follow the "<project>-<service>:<port>" convention used in prod (e.g. local dev). */
  private static String serviceName(String instance) {
    if (instance == null) return "unknown";
    String withoutPort = instance.contains(":") ? instance.substring(0, instance.indexOf(':')) : instance;
    int lastDash = withoutPort.lastIndexOf('-');
    return lastDash >= 0 ? withoutPort.substring(lastDash + 1) : withoutPort;
  }
}
