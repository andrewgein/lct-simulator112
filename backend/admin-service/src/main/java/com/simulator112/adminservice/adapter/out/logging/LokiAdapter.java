package com.simulator112.adminservice.adapter.out.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.simulator112.adminservice.application.port.out.LogQueryPort;
import com.simulator112.adminservice.domain.model.LogEntry;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class LokiAdapter implements LogQueryPort {

  private final RestClient restClient;

  public LokiAdapter(@Value("${loki.base-url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public List<LogEntry> recentLogs(String container, int limit) {
    return query(container, null, limit, "backward");
  }

  @Override
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

      Instant start = since != null ? since.plusNanos(1) : Instant.now().minus(Duration.ofDays(7));
      uri.append("&start=").append(toNanos(start)).append("&end=").append(toNanos(Instant.now()));

      JsonNode response = restClient.get().uri(URI.create(uri.toString())).retrieve().body(JsonNode.class);
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

  private static long toNanos(Instant instant) {
    return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
  }
}
