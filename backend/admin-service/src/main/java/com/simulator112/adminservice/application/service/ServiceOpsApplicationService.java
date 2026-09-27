package com.simulator112.adminservice.application.service;

import com.simulator112.adminservice.application.port.in.ManageServiceOpsUseCase;
import com.simulator112.adminservice.application.port.out.CiWorkflowPort;
import com.simulator112.adminservice.domain.model.ControllableServices;
import com.simulator112.adminservice.domain.model.LatestRun;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ServiceOpsApplicationService implements ManageServiceOpsUseCase {

  private static final Set<String> CONTROL_ACTIONS = Set.of("start", "stop", "restart");

  private final CiWorkflowPort ciWorkflowPort;

  @Override
  public List<ServiceOpsStatus> listServices() {
    return ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.keySet().stream()
        .map(
            service -> {
              LatestRun run =
                  ciWorkflowPort.latestRun(ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.get(service));
              return new ServiceOpsStatus(
                  service,
                  run != null ? run.status() : null,
                  run != null ? run.conclusion() : null,
                  run != null ? run.htmlUrl() : null);
            })
        .toList();
  }

  @Override
  public void act(String service, String action) {
    if (!ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.containsKey(service)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service: " + service);
    }
    if (CONTROL_ACTIONS.contains(action)) {
      ciWorkflowPort.dispatch(
          ControllableServices.CONTROL_WORKFLOW, Map.of("service", service, "action", action));
    } else if ("update".equals(action)) {
      ciWorkflowPort.dispatch(ControllableServices.UPDATE_WORKFLOW_BY_SERVICE.get(service), null);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + action);
    }
  }
}
