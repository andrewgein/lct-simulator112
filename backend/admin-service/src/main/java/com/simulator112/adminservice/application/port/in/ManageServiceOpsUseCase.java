package com.simulator112.adminservice.application.port.in;

import java.util.List;

public interface ManageServiceOpsUseCase {
  List<ServiceOpsStatus> listServices();

  void act(String service, String action);

  record ServiceOpsStatus(
      String service, String lastRunStatus, String lastRunConclusion, String lastRunUrl) {}
}
