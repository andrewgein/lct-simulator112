package com.simulator112.adminservice.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ControllableServices {

  public static final Map<String, String> UPDATE_WORKFLOW_BY_SERVICE = new LinkedHashMap<>();

  static {
    for (String service :
        new String[] {
          "auth-service", "incident-service", "course-service", "context-service",
          "review-service", "profile-service", "classifier-service", "notification-service",
          "admin-service", "dialog-service", "api-gateway", "frontend"
        }) {
      UPDATE_WORKFLOW_BY_SERVICE.put(service, service + ".yml");
    }
  }

  public static final String CONTROL_WORKFLOW = "service-control.yml";

  private ControllableServices() {}
}
