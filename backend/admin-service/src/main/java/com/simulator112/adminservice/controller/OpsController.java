package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.service.ControllableServices;
import com.simulator112.adminservice.service.GithubActionsClient;
import com.simulator112.adminservice.service.GithubActionsClient.LatestRun;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
public class OpsController {

  private static final Set<String> CONTROL_ACTIONS = Set.of("start", "stop", "restart");

  private final GithubActionsClient githubActionsClient;

  public OpsController(GithubActionsClient githubActionsClient) {
    this.githubActionsClient = githubActionsClient;
  }

  public record ServiceOpsStatus(String service, String lastRunStatus, String lastRunConclusion, String lastRunUrl) {}

  @GetMapping("/api/v1/admin/ops/services")
  public List<ServiceOpsStatus> listServices() {
    return ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.keySet().stream()
        .map(
            service -> {
              LatestRun run =
                  githubActionsClient.latestRun(ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.get(service));
              return new ServiceOpsStatus(
                  service,
                  run != null ? run.status() : null,
                  run != null ? run.conclusion() : null,
                  run != null ? run.htmlUrl() : null);
            })
        .toList();
  }

  @PostMapping("/api/v1/admin/ops/services/{service}/{action}")
  public Map<String, String> act(@PathVariable String service, @PathVariable String action) {
    if (!ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.containsKey(service)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service: " + service);
    }
    if (CONTROL_ACTIONS.contains(action)) {
      githubActionsClient.dispatch(
          ControllableServices.CONTROL_WORKFLOW, Map.of("service", service, "action", action));
    } else if ("update".equals(action)) {
      githubActionsClient.dispatch(ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.get(service), null);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + action);
    }
    return Map.of("service", service, "action", action, "status", "dispatched");
  }
}
