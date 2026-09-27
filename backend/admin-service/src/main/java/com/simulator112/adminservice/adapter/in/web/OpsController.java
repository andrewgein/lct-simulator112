package com.simulator112.adminservice.adapter.in.web;

import com.simulator112.adminservice.application.port.in.ManageServiceOpsUseCase;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpsController {

  private final ManageServiceOpsUseCase manageServiceOps;

  public OpsController(ManageServiceOpsUseCase manageServiceOps) {
    this.manageServiceOps = manageServiceOps;
  }

  @GetMapping("/api/v1/admin/ops/services")
  public List<ManageServiceOpsUseCase.ServiceOpsStatus> listServices() {
    return manageServiceOps.listServices();
  }

  @PostMapping("/api/v1/admin/ops/services/{service}/{action}")
  public Map<String, String> act(@PathVariable String service, @PathVariable String action) {
    manageServiceOps.act(service, action);
    return Map.of("service", service, "action", action, "status", "dispatched");
  }
}
