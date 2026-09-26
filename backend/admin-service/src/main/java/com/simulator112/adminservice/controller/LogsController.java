package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.LogEntryResponse;
import com.simulator112.adminservice.service.ContainerNames;
import com.simulator112.adminservice.service.LokiClient;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LogsController {

  private final LokiClient lokiClient;

  public LogsController(LokiClient lokiClient) {
    this.lokiClient = lokiClient;
  }

  @GetMapping("/api/v1/admin/logs/services")
  public List<String> services() {
    return List.copyOf(ContainerNames.CONTAINER_BY_SERVICE.keySet());
  }

  @GetMapping("/api/v1/admin/logs")
  public List<LogEntryResponse> logs(
      @RequestParam String service,
      @RequestParam(required = false) Instant since,
      @RequestParam(defaultValue = "200") int limit) {
    String container = ContainerNames.CONTAINER_BY_SERVICE.get(service);
    if (container == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service: " + service);
    }
    List<LokiClient.LogEntry> entries =
        since != null
            ? lokiClient.logsSince(container, since, Math.min(limit, 1000))
            : lokiClient.recentLogs(container, Math.min(limit, 1000));
    return entries.stream().map(e -> new LogEntryResponse(e.timestamp(), e.line())).toList();
  }
}
