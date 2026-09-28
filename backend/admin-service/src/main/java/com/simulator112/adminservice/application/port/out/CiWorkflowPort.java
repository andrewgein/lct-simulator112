package com.simulator112.adminservice.application.port.out;

import com.simulator112.adminservice.domain.model.LatestRun;
import java.util.Map;

public interface CiWorkflowPort {
  void dispatch(String workflowFile, Map<String, String> inputs);

  LatestRun latestRun(String workflowFile);
}
