package com.simulator112.adminservice.adapter.in.web;

import com.simulator112.adminservice.adapter.in.web.dto.LogEntryResponse;
import com.simulator112.adminservice.application.port.out.LogQueryPort;
import com.simulator112.adminservice.domain.model.ContainerNames;
import com.simulator112.adminservice.domain.model.LogEntry;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LogsController {

  private final LogQueryPort logQuery;

  public LogsController(LogQueryPort logQuery) {
    this.logQuery = logQuery;
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
    List<LogEntry> entries =
        since != null
            ? logQuery.logsSince(container, since, Math.min(limit, 1000))
            : logQuery.recentLogs(container, Math.min(limit, 1000));
    return entries.stream().map(e -> new LogEntryResponse(e.timestamp(), e.line())).toList();
  }
}
