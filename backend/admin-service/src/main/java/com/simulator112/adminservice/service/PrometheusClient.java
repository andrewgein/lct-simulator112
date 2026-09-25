package com.simulator112.adminservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Thin wrapper around Prometheus's instant-query HTTP API (/api/v1/query). Read-only. */
@Slf4j
@Component
public class PrometheusClient {

  private final RestClient restClient;

  public PrometheusClient(@Value("${prometheus.base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  /** Runs a PromQL instant query, returning each result vector's labels and scalar value. */
  public List<PrometheusSample> query(String promql) {
    List<PrometheusSample> samples = new ArrayList<>();
    try {
      String encoded = URLEncoder.encode(promql, StandardCharsets.UTF_8);
      JsonNode response =
          restClient
              .get()
              .uri(URI.create("/api/v1/query?query=" + encoded))
              .retrieve()
              .body(JsonNode.class);
      if (response == null) return samples;
      for (JsonNode result : response.path("data").path("result")) {
        JsonNode metric = result.path("metric");
        JsonNode value = result.path("value");
        double scalar = value.isArray() && value.size() == 2 ? value.get(1).asDouble() : Double.NaN;
        samples.add(new PrometheusSample(metric, scalar));
      }
    } catch (Exception e) {
      log.warn("Prometheus query failed: {} ({})", promql, e.getMessage());
    }
    return samples;
  }

  public record PrometheusSample(JsonNode labels, double value) {
    public String label(String name) {
      return labels.path(name).asText(null);
    }
  }
}
