package com.simulator112.adminservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Reads container logs back out of Loki (shipped there by Promtail - see
 * prod/monitoring/promtail-config.yaml). admin-service never touches docker.sock itself. */
@Slf4j
@Component
public class LokiClient {

  private final RestClient restClient;

  public LokiClient(@Value("${loki.base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  /** Last {@code limit} lines for a container, oldest first. */
  public List<LogEntry> recentLogs(String container, int limit) {
    return query(container, null, limit, "backward");
  }

  /** Lines strictly after {@code since}, oldest first - for polling only what's new. */
  public List<LogEntry> logsSince(String container, Instant since, int limit) {
    return query(container, since, limit, "forward");
  }

  private List<LogEntry> query(String container, Instant since, int limit, String direction) {
    List<LogEntry> entries = new ArrayList<>();
    try {
      String logql = URLEncoder.encode("{container=\"" + container + "\"}", StandardCharsets.UTF_8);
      StringBuilder uri =
          new StringBuilder("/loki/api/v1/query_range?query=")
              .append(logql)
              .append("&limit=")
              .append(limit)
              .append("&direction=")
              .append(direction);
      if (since != null) {
        // Loki wants nanosecond-precision unix timestamps for start/end.
        uri.append("&start=").append(since.getEpochSecond() * 1_000_000_000L + since.getNano() + 1)
           .append("&end=").append(Instant.now().getEpochSecond() * 1_000_000_000L);
      }
      JsonNode response = restClient.get().uri(uri.toString()).retrieve().body(JsonNode.class);
      if (response == null) return entries;
      for (JsonNode stream : response.path("data").path("result")) {
        for (JsonNode value : stream.path("values")) {
          long nanos = value.get(0).asLong();
          entries.add(new LogEntry(Instant.ofEpochSecond(0, nanos), value.get(1).asText()));
        }
      }
      entries.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
    } catch (Exception e) {
      log.warn("Failed to query Loki for container {}: {}", container, e.getMessage());
    }
    return entries;
  }

  public record LogEntry(Instant timestamp, String line) {}
}
