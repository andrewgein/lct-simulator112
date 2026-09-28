package com.simulator112.adminservice.adapter.out.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.simulator112.adminservice.application.port.out.MetricsQueryPort;
import com.simulator112.adminservice.domain.model.PrometheusSample;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PrometheusAdapter implements MetricsQueryPort {

  private final RestClient restClient;

  public PrometheusAdapter(@Value("${prometheus.base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public List<PrometheusSample> query(String promql) {
    List<PrometheusSample> samples = new ArrayList<>();
    try {
      String encoded = URLEncoder.encode(promql, StandardCharsets.UTF_8);
      JsonNode response =
          restClient.get().uri(URI.create("/api/v1/query?query=" + encoded)).retrieve().body(JsonNode.class);
      if (response == null) return samples;
      for (JsonNode result : response.path("data").path("result")) {
        JsonNode metric = result.path("metric");
        JsonNode value = result.path("value");
        double scalar = value.isArray() && value.size() == 2 ? value.get(1).asDouble() : Double.NaN;
        Map<String, String> labels = new LinkedHashMap<>();
        metric.fields().forEachRemaining(entry -> labels.put(entry.getKey(), entry.getValue().asText(null)));
        samples.add(new PrometheusSample(labels, scalar));
      }
    } catch (Exception e) {
      log.warn("Prometheus query failed: {} ({})", promql, e.getMessage());
    }
    return samples;
  }
}
