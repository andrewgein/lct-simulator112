package com.simulator112.adminservice.application.port.out;

import com.simulator112.adminservice.domain.model.LogEntry;
import java.time.Instant;
import java.util.List;

public interface LogQueryPort {
  List<LogEntry> recentLogs(String container, int limit);

  List<LogEntry> logsSince(String container, Instant since, int limit);
}
